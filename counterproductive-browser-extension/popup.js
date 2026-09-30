const status = document.getElementById("status");
const toggle = document.getElementById("toggle");

async function getActiveTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  return tab;
}

async function readState() {
  try {
    const tab = await getActiveTab();
    if (!tab?.id) {
      throw new Error("No active tab");
    }

    const state = await chrome.tabs.sendMessage(tab.id, { type: "get-state" });
    status.textContent = state.running
      ? `Flowers are appearing on this page. Open: ${state.count}.`
      : `Flowers are paused on this page. Open: ${state.count}.`;
    toggle.textContent = state.running ? "Stop flowers" : "Start flowers";
    toggle.dataset.running = String(state.running);
    toggle.disabled = false;
  } catch {
    status.textContent = "Open a regular web page, then reload it to enable flowers.";
    toggle.textContent = "Unavailable on this page";
    toggle.disabled = true;
  }
}

toggle.addEventListener("click", async () => {
  toggle.disabled = true;
  try {
    const tab = await getActiveTab();
    const running = toggle.dataset.running !== "true";
    await chrome.tabs.sendMessage(tab.id, { type: "set-running", running });
    await readState();
  } catch {
    status.textContent = "Could not reach this page. Reload it and try again.";
    toggle.disabled = true;
  }
});

readState();
