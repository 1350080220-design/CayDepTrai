/* VietGo — bản đồ lộ trình và trình phát hành trình (Leaflet) */
(function () {
  "use strict";

  var roots = document.querySelectorAll("[data-journey]");
  if (!roots.length) return;

  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var WEEKDAYS = ["Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"];
  var COLORS = { train: "#0B7A75", bus: "#D9920C" };
  var SEGMENTS = 10; // số điểm nội suy giữa hai nút để đường đi mềm mại

  Array.prototype.forEach.call(roots, function (root) {
    var data;
    try { data = JSON.parse(root.getAttribute("data-journey") || "{}"); } catch (e) { return; }
    if (!data || !data.stops || data.stops.length < 2) return;
    try { init(root, data); } catch (e) { root.classList.add("no-map"); }
  });

  function pad(n) { return (n < 10 ? "0" : "") + n; }

  function durationText(min) {
    min = Math.max(0, Math.round(min));
    var h = Math.floor(min / 60), m = min % 60;
    if (!h) return m + " phút";
    return m ? h + " giờ " + m + " phút" : h + " giờ";
  }

  /* ---------- Hình học: làm mềm đường đi (Catmull-Rom hướng tâm) ---------- */
  function lerpAt(a, b, ta, tb, t) {
    var w = (t - ta) / (tb - ta);
    return [a[0] + (b[0] - a[0]) * w, a[1] + (b[1] - a[1]) * w];
  }
  function knot(a, b) {
    var x = b[0] - a[0], y = b[1] - a[1];
    return Math.max(1e-6, Math.pow(x * x + y * y, 0.25));
  }
  function catmull(p0, p1, p2, p3, u) {
    var t0 = 0, t1 = t0 + knot(p0, p1), t2 = t1 + knot(p1, p2), t3 = t2 + knot(p2, p3);
    var t = t1 + (t2 - t1) * u;
    var a1 = lerpAt(p0, p1, t0, t1, t), a2 = lerpAt(p1, p2, t1, t2, t), a3 = lerpAt(p2, p3, t2, t3, t);
    var b1 = lerpAt(a1, a2, t0, t2, t), b2 = lerpAt(a2, a3, t1, t3, t);
    return lerpAt(b1, b2, t1, t2, t);
  }
  function smoothPath(pts) {
    var n = pts.length, out = [], idx = [];
    for (var i = 0; i < n - 1; i++) {
      var p1 = pts[i], p2 = pts[i + 1];
      var p0 = i > 0 ? pts[i - 1] : [2 * p1[0] - p2[0], 2 * p1[1] - p2[1]];
      var p3 = i + 2 < n ? pts[i + 2] : [2 * p2[0] - p1[0], 2 * p2[1] - p1[1]];
      idx.push(out.length);
      for (var s = 0; s < SEGMENTS; s++) out.push(catmull(p0, p1, p2, p3, s / SEGMENTS));
    }
    idx.push(out.length);
    out.push(pts[n - 1]);
    return { pts: out, idx: idx };
  }
  /* Khoảng cách tích lũy (km) dọc đường đi */
  function cumulative(pts) {
    var cum = [0];
    for (var i = 1; i < pts.length; i++) {
      var la = (pts[i][0] + pts[i - 1][0]) / 2 * Math.PI / 180;
      var dy = (pts[i][0] - pts[i - 1][0]) * 111.32;
      var dx = (pts[i][1] - pts[i - 1][1]) * 111.32 * Math.cos(la);
      cum.push(cum[i - 1] + Math.sqrt(dx * dx + dy * dy));
    }
    return cum;
  }

  function init(root, data) {
    var mode = data.mode === "bus" ? "bus" : "train";
    var total = Math.max(1, Number(data.total) || 1);
    var stops = data.stops;
    var last = stops.length - 1;
    var m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(data.dep || "");
    var depUtc = m ? Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]) : 0;

    var items = root.querySelectorAll(".jt-item");
    var timeline = root.querySelector(".journey-timeline");
    var clockEl = root.querySelector(".hud-clock");
    var dayEl = root.querySelector(".hud-day");
    var statusEl = root.querySelector(".hud-status");
    var playBtn = root.querySelector(".jc-play");
    var range = root.querySelector(".jc-range");
    var etaEl = root.querySelector(".jc-eta");
    var mapEl = root.querySelector(".journey-map");

    /* ---------- Bản đồ ---------- */
    var map = null, vehicle = null, progress = null, routeLines = [], markers = [];
    var smooth = null, cum = null, stopDist = [];
    var hasMap = !!(window.L && mapEl && data.path && data.path.length >= 2);

    if (hasMap) {
      smooth = smoothPath(data.path);
      cum = cumulative(smooth.pts);
      var pathLen = cum[cum.length - 1];
      stops.forEach(function (s) {
        if (s.i >= 0 && s.i < smooth.idx.length) stopDist.push(cum[smooth.idx[s.i]]);
        else stopDist.push(pathLen * (s.km || 0) / Math.max(1, stops[last].km || 1));
      });
      buildMap();
    } else {
      root.classList.add("no-map");
    }

    function buildMap() {
      var color = COLORS[mode];
      map = L.map(mapEl, { zoomControl: false, scrollWheelZoom: false, attributionControl: true });
      L.control.zoom({ position: "topright" }).addTo(map);
      L.tileLayer("https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}", {
        maxZoom: 18,
        attribution: 'Tiles &copy; <a href="https://www.esri.com">Esri</a>, HERE, Garmin, USGS, &copy; OpenStreetMap'
      }).addTo(map);

      var casing = L.polyline(smooth.pts, { color: "#fff", weight: 10, opacity: .95, lineCap: "round", lineJoin: "round", interactive: false }).addTo(map);
      var base = L.polyline(smooth.pts, { color: color, weight: 5, opacity: .32, lineCap: "round", lineJoin: "round", interactive: false }).addTo(map);
      progress = L.polyline([smooth.pts[0]], { color: color, weight: 5, opacity: 1, lineCap: "round", lineJoin: "round", interactive: false }).addTo(map);
      routeLines = [casing, base];

      stops.forEach(function (s, k) {
        var ll = latLngOf(k);
        var kind = s.k, html, size;
        if (kind === "REST") { html = '<span class="jm-stop rest"><i class="bi bi-cup-hot"></i></span>'; size = 26; }
        else if (kind === "ORIGIN" || kind === "DESTINATION") { html = '<span class="jm-stop end ' + mode + '"></span>'; size = 20; }
        else if (kind === "PASS") { html = '<span class="jm-stop pass ' + mode + '"></span>'; size = 10; }
        else { html = '<span class="jm-stop ' + mode + '"></span>'; size = 14; }
        var mk = L.marker(ll, {
          icon: L.divIcon({ className: "jm-icon", html: html, iconSize: [size, size], iconAnchor: [size / 2, size / 2] }),
          keyboard: false, riseOnHover: true
        }).addTo(map);
        var endpoint = kind === "ORIGIN" || kind === "DESTINATION";
        mk.bindTooltip(endpoint ? s.n : s.n + " · " + s.t, {
          direction: k === last ? "bottom" : "top", offset: [0, endpoint ? -12 : -8],
          className: "jm-tip" + (endpoint ? " end" : ""), permanent: endpoint
        });
        mk.on("click", function () { jumpTo(k, false); });
        markers.push(mk);
      });

      vehicle = L.marker(smooth.pts[0], {
        icon: L.divIcon({
          className: "jm-vehicle " + mode,
          html: '<span class="veh"><i class="bi ' + (mode === "bus" ? "bi-bus-front-fill" : "bi-train-front-fill") + '"></i></span>',
          iconSize: [40, 40], iconAnchor: [20, 20]
        }),
        zIndexOffset: 1000, interactive: false, keyboard: false
      }).addTo(map);

      map.fitBounds(base.getBounds(), { paddingTopLeft: [30, 96], paddingBottomRight: [30, 30], maxZoom: 11 });
      map.on("focus", function () { map.scrollWheelZoom.enable(); });
      map.on("blur", function () { map.scrollWheelZoom.disable(); });
    }

    function latLngOf(k) {
      var s = stops[k];
      if (smooth && s.i >= 0 && s.i < smooth.idx.length) return smooth.pts[smooth.idx[s.i]];
      return positionAt(stopDist[k] || 0);
    }

    function indexAt(d) {
      var lo = 0, hi = cum.length - 1;
      while (lo < hi) {
        var mid = (lo + hi) >> 1;
        if (cum[mid] < d) lo = mid + 1; else hi = mid;
      }
      return Math.max(1, lo);
    }
    function positionAt(d) {
      if (!cum) return null;
      if (d <= 0) return smooth.pts[0];
      if (d >= cum[cum.length - 1]) return smooth.pts[smooth.pts.length - 1];
      var i = indexAt(d);
      var seg = cum[i] - cum[i - 1] || 1;
      var w = (d - cum[i - 1]) / seg;
      var a = smooth.pts[i - 1], b = smooth.pts[i];
      return [a[0] + (b[0] - a[0]) * w, a[1] + (b[1] - a[1]) * w];
    }

    /* Vẽ dần tuyến đường khi bản đồ xuất hiện */
    function drawIn() {
      if (!map || reduceMotion) return;
      routeLines.forEach(function (line, n) {
        var el = line.getElement && line.getElement();
        if (!el || !el.getTotalLength) return;
        var len = el.getTotalLength();
        el.style.strokeDasharray = len + " " + len;
        el.style.strokeDashoffset = String(len);
        el.getBoundingClientRect();
        el.style.transition = "stroke-dashoffset 1.5s cubic-bezier(.2,.8,.2,1) " + (n * 0.08) + "s";
        el.style.strokeDashoffset = "0";
        var clear = function () { el.style.transition = ""; el.style.strokeDasharray = ""; el.style.strokeDashoffset = ""; };
        el.addEventListener("transitionend", clear, { once: true });
        map.once("zoomstart", clear);
        setTimeout(clear, 2600);
      });
    }

    /* ---------- Trạng thái theo thời gian ---------- */
    function phaseAt(t) {
      for (var k = 0; k <= last; k++) {
        var s = stops[k];
        if (t >= s.a && t <= s.d) return { at: k, from: k, to: k, f: 0 };
        if (k < last && t > s.d && t < stops[k + 1].a) {
          var span = stops[k + 1].a - s.d || 1;
          return { at: -1, from: k, to: k + 1, f: (t - s.d) / span };
        }
      }
      return { at: last, from: last, to: last, f: 0 };
    }
    function namedNeighbour(k, dir) {
      for (var j = k; j >= 0 && j <= last; j += dir) if (stops[j].k !== "PASS") return stops[j];
      return stops[dir < 0 ? 0 : last];
    }
    function statusText(p, t) {
      if (p.at >= 0) {
        var s = stops[p.at];
        if (s.k === "ORIGIN") return t <= 0 ? "Sẵn sàng khởi hành tại " + s.n : "Rời " + s.n;
        if (s.k === "DESTINATION") return "Đã đến " + s.n;
        if (s.k === "REST") return "Đang nghỉ tại " + s.n + " · " + s.w + " phút";
        if (s.k === "STATION") return "Dừng tại ga " + s.n + " · " + s.w + " phút";
      }
      var from = namedNeighbour(p.from, -1), to = namedNeighbour(p.to, 1);
      return (mode === "bus" ? "Đang chạy: " : "Tàu đang chạy: ") + from.n + " → " + to.n;
    }

    var cur = 0, lastReached = -1, playing = false, dragging = false, interacted = false;

    function render(t, follow) {
      cur = Math.max(0, Math.min(total, t));
      var p = phaseAt(cur);

      if (map) {
        var d = p.at >= 0 ? stopDist[p.at] : stopDist[p.from] + p.f * (stopDist[p.to] - stopDist[p.from]);
        var pos = positionAt(d);
        if (pos) {
          vehicle.setLatLng(pos);
          var upto = indexAt(d);
          var trail = smooth.pts.slice(0, upto);
          trail.push(pos);
          progress.setLatLngs(trail);
          if (follow && playing && !map.getBounds().pad(-0.12).contains(pos)) map.panTo(pos, { animate: true, duration: .6 });
        }
      }

      if (depUtc) {
        var now = new Date(depUtc + cur * 60000);
        if (clockEl) clockEl.textContent = pad(now.getUTCHours()) + ":" + pad(now.getUTCMinutes());
        if (dayEl) dayEl.textContent = WEEKDAYS[now.getUTCDay()] + ", " + pad(now.getUTCDate()) + "/" + pad(now.getUTCMonth() + 1);
      }
      if (statusEl) statusEl.textContent = statusText(p, cur);
      if (etaEl) etaEl.textContent = cur >= total ? "Đã đến nơi" : (cur <= 0 ? "Tổng " + durationText(total) : "Còn " + durationText(total - cur));
      if (range && !dragging) range.value = String(Math.round(cur / total * 1000));

      var reached = p.at >= 0 ? p.at : p.from;
      if (reached !== lastReached) {
        lastReached = reached;
        Array.prototype.forEach.call(items, function (li, k) {
          li.classList.toggle("is-past", k < reached || (k === reached && p.at < 0));
          li.classList.toggle("is-current", k === reached);
        });
        if (follow && timeline && items[reached]) {
          var target = items[reached].offsetTop - timeline.clientHeight / 2 + items[reached].offsetHeight / 2;
          if (timeline.scrollHeight > timeline.clientHeight) timeline.scrollTo({ top: target, behavior: reduceMotion ? "auto" : "smooth" });
        }
      }
    }

    /* ---------- Phát hành trình ---------- */
    var duration = Math.min(20000, Math.max(9000, 8000 + stops.length * 380));
    var HOLD = 650;
    var lastTs = 0, hold = null, held = {};

    function setPlayIcon() {
      if (!playBtn) return;
      var icon = playing ? "bi-pause-fill" : (cur >= total ? "bi-arrow-repeat" : "bi-play-fill");
      var label = playing ? "Tạm dừng" : (cur >= total ? "Phát lại hành trình" : "Phát hành trình");
      playBtn.innerHTML = '<i class="bi ' + icon + '"></i>';
      playBtn.setAttribute("aria-label", label);
      root.classList.toggle("is-playing", playing);
    }

    function pulse(k) {
      var el = markers[k] && markers[k].getElement && markers[k].getElement();
      var dot = el && el.querySelector(".jm-stop");
      if (dot) { dot.classList.remove("hit"); void dot.offsetWidth; dot.classList.add("hit"); }
      if (markers[k] && stops[k].k !== "PASS") markers[k].openTooltip();
    }

    function step(ts) {
      if (!playing) return;
      if (!lastTs) lastTs = ts;
      var dt = Math.min(64, ts - lastTs);
      lastTs = ts;

      if (hold) {
        var hp = Math.min(1, (ts - hold.start) / HOLD);
        render(stops[hold.k].a + (stops[hold.k].d - stops[hold.k].a) * hp, true);
        if (hp >= 1) {
          if (markers[hold.k] && stops[hold.k].k !== "DESTINATION" && stops[hold.k].k !== "ORIGIN") markers[hold.k].closeTooltip();
          hold = null;
        }
        requestAnimationFrame(step);
        return;
      }

      var next = cur + dt * total / duration;
      for (var k = 1; k < last; k++) {
        var s = stops[k];
        if (!held[k] && (s.k === "REST" || s.w >= 10) && s.a > cur && s.a <= next) {
          held[k] = true;
          hold = { k: k, start: ts };
          render(s.a, true);
          pulse(k);
          requestAnimationFrame(step);
          return;
        }
      }
      render(next, true);
      if (cur >= total) {
        playing = false;
        pulse(last);
        setPlayIcon();
        return;
      }
      requestAnimationFrame(step);
    }

    function play() {
      if (cur >= total) { cur = 0; held = {}; lastReached = -1; }
      playing = true;
      lastTs = 0;
      hold = null;
      setPlayIcon();
      requestAnimationFrame(step);
    }
    function pause() {
      playing = false;
      hold = null;
      setPlayIcon();
    }

    if (playBtn) playBtn.addEventListener("click", function () { interacted = true; if (playing) pause(); else play(); });

    if (range) {
      range.addEventListener("input", function () {
        interacted = true;
        if (playing) pause();
        dragging = true;
        held = {};
        render(Number(range.value) / 1000 * total, false);
        dragging = false;
        setPlayIcon();
      });
    }

    function jumpTo(k, pan) {
      interacted = true;
      pause();
      held = {};
      for (var j = 0; j <= k; j++) held[j] = true;
      render(stops[k].a, false);
      setPlayIcon();
      if (map) {
        if (pan) map.panTo(latLngOf(k), { animate: !reduceMotion });
        pulse(k);
      }
    }
    Array.prototype.forEach.call(items, function (li) {
      var btn = li.querySelector(".jt-btn");
      var k = Number(li.getAttribute("data-idx"));
      if (!btn || isNaN(k) || !stops[k]) return;
      btn.addEventListener("click", function () { jumpTo(k, true); });
      btn.addEventListener("mouseenter", function () { if (map && markers[k] && !playing) markers[k].openTooltip(); });
      btn.addEventListener("mouseleave", function () {
        var kind = stops[k].k;
        if (map && markers[k] && !playing && kind !== "ORIGIN" && kind !== "DESTINATION") markers[k].closeTooltip();
      });
    });

    render(0, false);
    setPlayIcon();

    /* Tự phát một lần khi người dùng cuộn tới bản đồ */
    if ("IntersectionObserver" in window) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (e) {
          if (!e.isIntersecting) return;
          io.disconnect();
          if (map) map.invalidateSize();
          drawIn();
          if (!reduceMotion && !interacted) setTimeout(function () { if (!interacted && !playing && cur === 0) play(); }, map ? 1300 : 300);
        });
      }, { threshold: 0.35 });
      io.observe(root);
    }
  }
})();
