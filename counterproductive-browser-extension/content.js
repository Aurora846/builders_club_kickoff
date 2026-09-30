(() => {
  if (globalThis.counterproductiveFlowers) {
    return;
  }

  const BASE_WIDTH = 190;
  const BASE_HEIGHT = 210;
  const FLOWER_COLORS = ["#ef5b77", "#f6ae2d", "#9b71bf", "#ebdc68", "#ee8249"];
  const host = document.createElement("div");
  host.id = "counterproductive-flowers-host";
  host.style.cssText = "position:fixed;inset:0;z-index:2147483647;pointer-events:none;";
  document.documentElement.appendChild(host);

  const shadow = host.attachShadow({ mode: "open" });
  const style = document.createElement("style");
  style.textContent = `
    .flower-window {
      position: fixed;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border: 1px solid #d6cdb8;
      border-radius: 7px;
      background: #fffaf0;
      box-shadow: 0 5px 22px #17201855;
      pointer-events: auto;
      user-select: none;
      animation: flower-arrive 180ms ease-out;
      transition: left 420ms ease-out, top 420ms ease-out;
    }
    .flower-window.jumping { transition: none !important; }
    .flower-art {
      display: grid;
      place-items: center;
      background: #fffaf0;
      touch-action: none;
    }
    .flower-art.draggable { cursor: move; }
    .flower-art svg { display: block; }
    .flower-close {
      min-height: 34px;
      border: 0;
      background: #f4efe3;
      color: #303c33;
      font: 600 13px "Segoe UI", sans-serif;
      cursor: pointer;
    }
    .flower-close.top { border-bottom: 1px solid #e2dac8; }
    .flower-close.bottom { border-top: 1px solid #e2dac8; }
    .flower-close:hover { background: #e9e0cf; }
    @keyframes flower-arrive {
      from { opacity: 0; transform: scale(.94); }
      to { opacity: 1; transform: scale(1); }
    }
  `;
  shadow.appendChild(style);

  let running = false;
  let timerId = null;
  const flowers = new Set();
  const lastDodgeTimes = new WeakMap();

  function randomBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  function getFlowerSize() {
    // CSS pixels use a standard 96-DPI reference, so one centimeter is about 38 px.
    const centimeterInCssPixels = Math.round(96 / 2.54);
    const adjustment = Math.min(centimeterInCssPixels, Math.round(BASE_WIDTH * 0.25));
    const width = BASE_WIDTH + randomBetween(-adjustment, adjustment);
    const scale = width / BASE_WIDTH;
    return { width, height: Math.round(BASE_HEIGHT * scale) };
  }

  function randomizeFlowerPosition(flowerWindow) {
    const maxX = Math.max(0, window.innerWidth - flowerWindow.offsetWidth);
    const maxY = Math.max(0, window.innerHeight - flowerWindow.offsetHeight);
    flowerWindow.style.left = `${randomBetween(0, maxX)}px`;
    flowerWindow.style.top = `${randomBetween(0, maxY)}px`;
  }

  function dodgeCloseButtons(event) {
    const now = Date.now();
    for (const flowerWindow of flowers) {
      const closeButton = flowerWindow.querySelector(".flower-close");
      const bounds = closeButton.getBoundingClientRect();
      const buttonX = bounds.left + bounds.width / 2;
      const buttonY = bounds.top + bounds.height / 2;
      const dx = buttonX - event.clientX;
      const dy = buttonY - event.clientY;
      const distance = Math.hypot(dx, dy);

      if (distance > 85 || now - (lastDodgeTimes.get(flowerWindow) ?? 0) < 650) {
        continue;
      }

      lastDodgeTimes.set(flowerWindow, now);
      const directionX = distance === 0 ? (Math.random() < 0.5 ? -1 : 1) : dx / distance;
      const directionY = distance === 0 ? 0 : dy / distance;
      const maxX = Math.max(0, window.innerWidth - flowerWindow.offsetWidth);
      const maxY = Math.max(0, window.innerHeight - flowerWindow.offsetHeight);
      const x = Math.max(0, Math.min(flowerWindow.offsetLeft + directionX * 48, maxX));
      const y = Math.max(0, Math.min(flowerWindow.offsetTop + directionY * 48, maxY));
      flowerWindow.style.left = `${x}px`;
      flowerWindow.style.top = `${y}px`;
    }
  }

  document.addEventListener("pointermove", dodgeCloseButtons, { passive: true });

  function makeArtwork(color, width, height) {
    const art = document.createElement("div");
    art.className = "flower-art";
    art.style.width = `${width}px`;
    art.style.height = `${height}px`;
    art.innerHTML = `
      <svg width="${width}" height="${height}" viewBox="0 0 ${BASE_WIDTH} ${BASE_HEIGHT}" role="img" aria-label="A colorful flower">
        <g fill="#438753">
          <path d="M95 99 C93 132 98 158 95 190" fill="none" stroke="#438753" stroke-width="5" stroke-linecap="round"/>
          <ellipse cx="79" cy="150" rx="18" ry="7" transform="rotate(-24 79 150)"/>
          <ellipse cx="111" cy="166" rx="18" ry="7" transform="rotate(24 111 166)"/>
        </g>
        <g fill="${color}">
          ${Array.from({ length: 10 }, (_, index) => `<ellipse cx="95" cy="49" rx="12" ry="21" transform="rotate(${index * 36} 95 82)"/>`).join("")}
        </g>
        <circle cx="95" cy="82" r="15" fill="#f5c339"/>
        <g fill="#9b691f">
          <circle cx="92" cy="77" r="1.5"/><circle cx="99" cy="84" r="1.5"/><circle cx="89" cy="86" r="1.5"/>
        </g>
      </svg>`;
    return art;
  }

  function makeDraggable(art, flowerWindow) {
    art.classList.add("draggable");
    art.title = "Drag this flower around";
    let dragOffset = null;

    art.addEventListener("pointerdown", event => {
      dragOffset = {
        x: event.clientX - flowerWindow.offsetLeft,
        y: event.clientY - flowerWindow.offsetTop
      };
      art.setPointerCapture(event.pointerId);
    });

    art.addEventListener("pointermove", event => {
      if (!dragOffset || event.buttons === 0) {
        return;
      }
      const maxX = Math.max(0, window.innerWidth - flowerWindow.offsetWidth);
      const maxY = Math.max(0, window.innerHeight - flowerWindow.offsetHeight);
      const x = Math.max(0, Math.min(event.clientX - dragOffset.x, maxX));
      const y = Math.max(0, Math.min(event.clientY - dragOffset.y, maxY));
      flowerWindow.style.left = `${x}px`;
      flowerWindow.style.top = `${y}px`;
    });

    const stopDragging = () => { dragOffset = null; };
    art.addEventListener("pointerup", stopDragging);
    art.addEventListener("pointercancel", stopDragging);
  }

  function showFlower() {
    const { width, height } = getFlowerSize();
    const flowerWindow = document.createElement("section");
    flowerWindow.className = "flower-window";
    flowerWindow.setAttribute("aria-label", "Flower interruption");

    const art = makeArtwork(FLOWER_COLORS[randomBetween(0, FLOWER_COLORS.length - 1)], width, height);
    if (Math.random() < 0.5) {
      makeDraggable(art, flowerWindow);
    } else {
      art.title = "This flower cannot be moved";
    }

    const closeButton = document.createElement("button");
    closeButton.className = "flower-close";
    closeButton.type = "button";
    const closeAtTop = Math.random() < 0.5;
    closeButton.classList.add(closeAtTop ? "top" : "bottom");
    let clicksRemaining = randomBetween(1, 4);
    const updateCloseLabel = () => {
      closeButton.textContent = clicksRemaining === 1
        ? "Close flower"
        : `Close flower (${clicksRemaining} clicks left)`;
      closeButton.setAttribute("aria-label", closeButton.textContent);
    };
    updateCloseLabel();
    closeButton.addEventListener("click", () => {
      clicksRemaining -= 1;
      if (clicksRemaining === 0) {
        flowerWindow.remove();
        flowers.delete(flowerWindow);
      } else {
        updateCloseLabel();
        flowerWindow.classList.add("jumping");
        randomizeFlowerPosition(flowerWindow);
        requestAnimationFrame(() => {
          requestAnimationFrame(() => flowerWindow.classList.remove("jumping"));
        });
      }
    });

    if (closeAtTop) {
      flowerWindow.append(closeButton, art);
    } else {
      flowerWindow.append(art, closeButton);
    }
    shadow.appendChild(flowerWindow);
    randomizeFlowerPosition(flowerWindow);
    flowers.add(flowerWindow);
  }

  function scheduleNextFlower() {
    if (!running) {
      return;
    }
    timerId = window.setTimeout(() => {
      showFlower();
      scheduleNextFlower();
    }, randomBetween(2000, 10000));
  }

  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === "get-state") {
      sendResponse({ running, count: flowers.size });
      return;
    }

    if (message.type === "set-running") {
      running = Boolean(message.running);
      window.clearTimeout(timerId);
      timerId = null;
      if (running) {
        scheduleNextFlower();
      }
      sendResponse({ running, count: flowers.size });
    }
  });

  globalThis.counterproductiveFlowers = true;
})();
