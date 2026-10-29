/* VietGo — hiệu ứng & tương tác chung */
(function () {
  "use strict";

  var reduceMotion = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function $(sel, root) { return (root || document).querySelector(sel); }
  function $$(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }

  window.VG = window.VG || {};
  VG.money = function (n) {
    n = Math.round(Number(n) || 0);
    var s = Math.abs(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    return (n < 0 ? "−" : "") + s + "đ";
  };

  /* Navbar đổi nền khi cuộn */
  var nav = $(".site-nav");
  if (nav) {
    var onScroll = function () { nav.classList.toggle("scrolled", window.scrollY > 12); };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
  }

  /* Hiện dần khi cuộn tới */
  var reveals = $$(".reveal");
  if (reveals.length) {
    if (!("IntersectionObserver" in window) || reduceMotion) {
      reveals.forEach(function (el) { el.classList.add("in"); });
    } else {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (e) {
          if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
        });
      }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });
      reveals.forEach(function (el) { io.observe(el); });
    }
  }

  /* Hiệu ứng gợn sóng khi bấm nút */
  document.addEventListener("click", function (e) {
    var btn = e.target.closest(".btn");
    if (!btn || reduceMotion || btn.disabled) return;
    var r = btn.getBoundingClientRect();
    var d = Math.max(r.width, r.height);
    var s = document.createElement("span");
    s.className = "ripple";
    s.style.width = s.style.height = d + "px";
    s.style.left = (e.clientX - r.left - d / 2) + "px";
    s.style.top = (e.clientY - r.top - d / 2) + "px";
    btn.appendChild(s);
    setTimeout(function () { s.remove(); }, 650);
  });

  /* Thông báo (toast) */
  function dismiss(t) {
    if (!t || t.classList.contains("leaving")) return;
    t.classList.add("leaving");
    setTimeout(function () { t.remove(); }, 360);
  }
  $$(".toast-x").forEach(function (t, i) {
    var close = t.querySelector("button");
    if (close) close.addEventListener("click", function () { dismiss(t); });
    setTimeout(function () { dismiss(t); }, 6000 + i * 800);
  });
  VG.toast = function (msg, kind) {
    var stack = $(".toast-stack");
    if (!stack) { stack = document.createElement("div"); stack.className = "toast-stack"; document.body.appendChild(stack); }
    var t = document.createElement("div");
    t.className = "toast-x " + (kind || "success");
    t.setAttribute("role", "status");
    var icon = kind === "error" ? "bi-exclamation-octagon-fill" : "bi-check-circle-fill";
    t.innerHTML = '<i class="bi ' + icon + '"></i><div></div><button type="button" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>';
    t.querySelector("div").textContent = msg;
    t.querySelector("button").addEventListener("click", function () { dismiss(t); });
    stack.appendChild(t);
    setTimeout(function () { dismiss(t); }, 5000);
  };

  /* Nút gửi form: trạng thái đang xử lý, chống bấm 2 lần */
  $$("form[data-loading]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      if (form.dataset.submitted === "1") { e.preventDefault(); return; }
      if (typeof form.checkValidity === "function" && !form.checkValidity()) return;
      form.dataset.submitted = "1";
      var btn = e.submitter || form.querySelector("button[type=submit]");
      if (btn) btn.classList.add("is-loading");
    });
  });

  /* Quay lại trang bằng nút Back: bỏ trạng thái đang xử lý của nút */
  window.addEventListener("pageshow", function () {
    $$("form[data-submitted]").forEach(function (f) { f.dataset.submitted = ""; });
    $$(".is-loading").forEach(function (b) { b.classList.remove("is-loading"); });
  });

  /* Xác nhận trước khi thực hiện thao tác quan trọng */
  $$("form[data-confirm]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      if (!window.confirm(form.getAttribute("data-confirm"))) {
        e.preventDefault();
        e.stopImmediatePropagation();
        form.dataset.submitted = "";
        $$(".is-loading", form).forEach(function (b) { b.classList.remove("is-loading"); });
      }
    }, true);
  });

  /* Hiện / ẩn mật khẩu */
  $$(".toggle-pass").forEach(function (b) {
    b.addEventListener("click", function () {
      var input = b.parentElement.querySelector("input");
      var show = input.type === "password";
      input.type = show ? "text" : "password";
      b.innerHTML = show ? '<i class="bi bi-eye-slash"></i>' : '<i class="bi bi-eye"></i>';
      b.setAttribute("aria-label", show ? "Ẩn mật khẩu" : "Hiện mật khẩu");
    });
  });

  /* Thước đo độ mạnh mật khẩu */
  $$("[data-pw-meter]").forEach(function (input) {
    var meter = document.getElementById(input.getAttribute("data-pw-meter"));
    var rules = document.getElementById(input.getAttribute("data-pw-rules"));
    function update() {
      var v = input.value || "";
      var checks = { len: v.length >= 8, lower: /[a-z]/.test(v), upper: /[A-Z]/.test(v), digit: /\d/.test(v) };
      var score = 0;
      Object.keys(checks).forEach(function (k) { if (checks[k]) score++; });
      if (meter) meter.setAttribute("data-score", v ? String(score) : "0");
      if (rules) $$("li", rules).forEach(function (li) { li.classList.toggle("ok", !!checks[li.dataset.rule]); });
    }
    input.addEventListener("input", update);
    update();
  });

  /* Đếm số tăng dần */
  $$("[data-count]").forEach(function (el) {
    var target = Number(el.getAttribute("data-count")) || 0;
    if (reduceMotion || target === 0) { el.textContent = target.toLocaleString("vi-VN"); return; }
    var start = null, dur = 1200;
    function step(ts) {
      if (!start) start = ts;
      var p = Math.min(1, (ts - start) / dur);
      var eased = 1 - Math.pow(1 - p, 3);
      el.textContent = Math.round(target * eased).toLocaleString("vi-VN");
      if (p < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  });

  /* Tab chọn loại vé ở trang chủ */
  var kindTabs = $(".kind-tabs");
  if (kindTabs) {
    $$(".kind-tab", kindTabs).forEach(function (tab) {
      tab.addEventListener("click", function () {
        var k = tab.getAttribute("data-target");
        kindTabs.setAttribute("data-active", k);
        $$(".kind-tab", kindTabs).forEach(function (t) {
          var on = t === tab;
          t.classList.toggle("active", on);
          t.setAttribute("aria-selected", on ? "true" : "false");
        });
        $$(".search-pane").forEach(function (p) { p.classList.toggle("show", p.id === "pane-" + k); });
      });
    });
  }

  /* Nút đảo chiều điểm đi - điểm đến */
  $$("[data-swap]").forEach(function (b) {
    b.addEventListener("click", function () {
      var ids = b.getAttribute("data-swap").split(",");
      var a = document.getElementById(ids[0]), c = document.getElementById(ids[1]);
      if (!a || !c) return;
      var tmp = a.value; a.value = c.value; c.value = tmp;
    });
  });

  /* Kiểm tra điểm đi khác điểm đến trước khi tìm */
  $$("form[data-od]").forEach(function (form) {
    form.addEventListener("submit", function (e) {
      var ids = form.getAttribute("data-od").split(",");
      var a = document.getElementById(ids[0]), c = document.getElementById(ids[1]);
      if (a && c && a.value && a.value === c.value) {
        e.preventDefault();
        VG.toast("Điểm đi và điểm đến phải khác nhau.", "error");
        c.focus();
      }
    });
  });

  /* Đồng hồ đếm ngược giữ chỗ */
  $$("[data-countdown]").forEach(function (ring) {
    var left = Number(ring.getAttribute("data-countdown")) || 0;
    var total = Number(ring.getAttribute("data-total")) || left || 1;
    var fg = ring.querySelector(".fg");
    var lbl = ring.querySelector(".lbl");
    var circ = fg ? 2 * Math.PI * Number(fg.getAttribute("r")) : 0;
    if (fg) { fg.style.strokeDasharray = circ; }
    function render() {
      var m = Math.floor(left / 60), s = left % 60;
      if (lbl) lbl.textContent = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
      if (fg) fg.style.strokeDashoffset = circ * (1 - left / total);
      ring.classList.toggle("urgent", left <= 60);
    }
    render();
    var timer = setInterval(function () {
      left = Math.max(0, left - 1);
      render();
      if (left === 0) {
        clearInterval(timer);
        VG.toast("Đã hết thời gian giữ chỗ. Trang sẽ tải lại.", "error");
        setTimeout(function () { window.location.reload(); }, 1800);
      }
    }, 1000);
  });

  /* Pháo giấy khi thanh toán thành công */
  VG.confetti = function () {
    if (reduceMotion) return;
    var colors = ["#0E9690", "#F2AE2B", "#14213D", "#C93A4E", "#6BE3A6"];
    var box = document.createElement("div");
    box.className = "confetti";
    for (var i = 0; i < 90; i++) {
      var p = document.createElement("i");
      p.style.left = Math.random() * 100 + "vw";
      p.style.background = colors[i % colors.length];
      p.style.animationDuration = (2.2 + Math.random() * 2) + "s";
      p.style.animationDelay = (Math.random() * .6) + "s";
      p.style.transform = "rotate(" + Math.random() * 360 + "deg)";
      box.appendChild(p);
    }
    document.body.appendChild(box);
    setTimeout(function () { box.remove(); }, 5200);
  };
  if (document.body.hasAttribute("data-celebrate")) VG.confetti();

  /* Chỉ cho nhập chữ số */
  $$("[data-digits]").forEach(function (input) {
    input.addEventListener("input", function () {
      var pos = input.selectionStart;
      var v = input.value.replace(/\D/g, "");
      if (v !== input.value) { input.value = v; try { input.setSelectionRange(pos - 1, pos - 1); } catch (ignored) { /* noop */ } }
    });
  });
})();
