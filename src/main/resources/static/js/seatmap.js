/* VietGo — chọn chỗ trên sơ đồ toa tàu / xe khách */
(function () {
  "use strict";
  var root = document.querySelector("[data-seatmap]");
  if (!root) return;

  function $$(sel, r) { return Array.prototype.slice.call((r || document).querySelectorAll(sel)); }

  var max = Number(root.getAttribute("data-max")) || 6;
  var unit = root.getAttribute("data-unit") || "chỗ";
  var form = document.getElementById("seat-form");
  var hidden = document.getElementById("seat-inputs");
  var list = document.getElementById("picked-list");
  var totalEl = document.getElementById("picked-total");
  var countEl = document.getElementById("picked-count");
  var submit = document.getElementById("seat-submit");
  var emptyNote = document.getElementById("picked-empty");
  var selected = []; // [{key,label,price,el}]

  /* Tooltip giá ghế */
  var tip = document.createElement("div");
  tip.className = "seat-tip";
  document.body.appendChild(tip);
  function showTip(seat) {
    var taken = seat.classList.contains("taken");
    tip.innerHTML = "";
    var t = document.createElement("div");
    t.textContent = seat.getAttribute("data-label");
    var p = document.createElement("div");
    if (taken) { p.textContent = "Đã có người đặt"; }
    else { p.innerHTML = "Giá người lớn: <b></b>"; p.querySelector("b").textContent = VG.money(seat.getAttribute("data-price")); }
    tip.appendChild(t); tip.appendChild(p);
    var r = seat.getBoundingClientRect();
    tip.classList.add("show");
    var tw = tip.offsetWidth;
    var left = Math.min(window.innerWidth - tw - 8, Math.max(8, r.left + r.width / 2 - tw / 2));
    tip.style.left = left + "px";
    tip.style.top = Math.max(8, r.top - tip.offsetHeight - 10) + "px";
  }
  function hideTip() { tip.classList.remove("show"); }

  function animateTotal(to) {
    var from = Number(totalEl.getAttribute("data-v")) || 0;
    totalEl.setAttribute("data-v", String(to));
    var start = null;
    function step(ts) {
      if (!start) start = ts;
      var p = Math.min(1, (ts - start) / 400);
      totalEl.textContent = VG.money(from + (to - from) * (1 - Math.pow(1 - p, 3)));
      if (p < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  function render() {
    hidden.innerHTML = "";
    list.innerHTML = "";
    var total = 0;
    selected.forEach(function (s) {
      total += s.price;
      var inp = document.createElement("input");
      inp.type = "hidden"; inp.name = "seats"; inp.value = s.key;
      hidden.appendChild(inp);

      var li = document.createElement("li");
      var name = document.createElement("span");
      name.textContent = s.label;
      var right = document.createElement("span");
      right.className = "d-inline-flex align-items-center gap-2";
      var price = document.createElement("strong");
      price.className = "num";
      price.textContent = VG.money(s.price);
      var rm = document.createElement("button");
      rm.type = "button";
      rm.setAttribute("aria-label", "Bỏ chọn " + s.label);
      rm.innerHTML = '<i class="bi bi-x-lg"></i>';
      rm.addEventListener("click", function () { toggle(s.el); });
      right.appendChild(price); right.appendChild(rm);
      li.appendChild(name); li.appendChild(right);
      list.appendChild(li);
    });
    countEl.textContent = selected.length + "/" + max;
    animateTotal(total);
    if (submit) submit.disabled = selected.length === 0;
    if (emptyNote) emptyNote.style.display = selected.length ? "none" : "";

    /* số chỗ đang chọn trên từng toa */
    $$("[data-coach-tab]").forEach(function (tab) {
      var no = tab.getAttribute("data-coach-tab");
      var n = selected.filter(function (s) { return s.coach === no; }).length;
      var b = tab.querySelector(".badge-sel");
      if (b) { b.textContent = n; b.classList.toggle("show", n > 0); }
      var car = document.querySelector('.train-strip .car[data-coach="' + no + '"]');
      if (car) car.classList.toggle("has-sel", n > 0);
    });
  }

  function toggle(seat) {
    if (!seat || seat.classList.contains("taken") || seat.disabled) return;
    var key = seat.getAttribute("data-key");
    var idx = -1;
    selected.forEach(function (s, i) { if (s.key === key) idx = i; });
    if (idx >= 0) {
      selected.splice(idx, 1);
      seat.classList.remove("selected");
      seat.setAttribute("aria-pressed", "false");
    } else {
      if (selected.length >= max) {
        VG.toast("Mỗi lần đặt tối đa " + max + " " + unit + ".", "error");
        return;
      }
      selected.push({
        key: key,
        label: seat.getAttribute("data-label"),
        price: Number(seat.getAttribute("data-price")) || 0,
        coach: seat.getAttribute("data-coach") || "",
        el: seat
      });
      seat.classList.add("selected");
      seat.setAttribute("aria-pressed", "true");
    }
    render();
  }

  $$(".seat", root).forEach(function (seat) {
    seat.addEventListener("click", function () { toggle(seat); });
    seat.addEventListener("mouseenter", function () { showTip(seat); });
    seat.addEventListener("mouseleave", hideTip);
    seat.addEventListener("focus", function () { showTip(seat); });
    seat.addEventListener("blur", hideTip);
  });
  window.addEventListener("scroll", hideTip, { passive: true });

  /* Chuyển toa */
  function showCoach(no) {
    $$("[data-coach-tab]").forEach(function (t) {
      var on = t.getAttribute("data-coach-tab") === no;
      t.classList.toggle("active", on);
      t.setAttribute("aria-selected", on ? "true" : "false");
    });
    $$(".train-strip .car").forEach(function (c) { c.classList.toggle("active", c.getAttribute("data-coach") === no); });
    $$(".coach-pane").forEach(function (p) { p.classList.toggle("show", p.getAttribute("data-coach-pane") === no); });
  }
  $$("[data-coach-tab]").forEach(function (t) {
    t.addEventListener("click", function () { showCoach(t.getAttribute("data-coach-tab")); });
  });
  $$(".train-strip .car").forEach(function (c) {
    c.addEventListener("click", function () { showCoach(c.getAttribute("data-coach")); });
  });

  /* Gợi ý chỗ ngồi liền nhau (gọi API) */
  var suggestBtn = document.getElementById("suggest-btn");
  if (suggestBtn) {
    suggestBtn.addEventListener("click", function () {
      var type = document.getElementById("suggest-type").value;
      var n = document.getElementById("suggest-n").value;
      var url = suggestBtn.getAttribute("data-url") + "?n=" + encodeURIComponent(n) + (type ? "&type=" + encodeURIComponent(type) : "");
      suggestBtn.classList.add("is-loading");
      fetch(url, { headers: { "Accept": "application/json" } })
        .then(function (r) { if (!r.ok) throw new Error(); return r.json(); })
        .then(function (data) {
          if (!data.found || !data.seats.length) {
            VG.toast("Không còn đủ " + n + " chỗ liền nhau cho hạng toa này.", "error");
            return;
          }
          /* bỏ chọn hiện tại rồi chọn theo gợi ý */
          selected.slice().forEach(function (s) { toggle(s.el); });
          var first = null;
          data.seats.forEach(function (k) {
            var el = root.querySelector('.seat[data-key="' + k + '"]');
            if (el) { toggle(el); el.classList.remove("suggested"); void el.offsetWidth; el.classList.add("suggested"); first = first || el; }
          });
          if (first) {
            showCoach(first.getAttribute("data-coach"));
            first.scrollIntoView({ behavior: "smooth", block: "center", inline: "center" });
          }
          VG.toast("Đã chọn " + data.seats.length + " chỗ gần nhau nhất.", "success");
        })
        .catch(function () { VG.toast("Không lấy được gợi ý. Vui lòng thử lại.", "error"); })
        .finally(function () { suggestBtn.classList.remove("is-loading"); });
    });
  }

  if (form) {
    form.addEventListener("submit", function (e) {
      if (selected.length === 0) {
        e.preventDefault();
        VG.toast("Vui lòng chọn ít nhất 1 " + unit + ".", "error");
        return;
      }
      if (submit) submit.classList.add("is-loading");
    });
  }
  render();
})();
