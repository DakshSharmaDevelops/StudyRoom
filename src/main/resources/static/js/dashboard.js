const navLinks = [...document.querySelectorAll(".nav-link[data-view]")];
const panels = [...document.querySelectorAll(".view-panel")];
const breadcrumb = document.querySelector("#breadcrumb-current");
const modal = document.querySelector("#modal-backdrop");
const modalTitle = document.querySelector("#modal-title");
const modalCopy = document.querySelector("#modal-copy");
const sidebar = document.querySelector("#sidebar");
let toastTimer;

function showView(name) {
    const selected = navLinks.find((link) => link.dataset.view === name);
    if (!selected) return;
    navLinks.forEach((link) => link.classList.toggle("active", link === selected));
    panels.forEach((panel) => panel.classList.toggle("active", panel.id === `view-${name}`));
    breadcrumb.textContent = selected.textContent.trim();
    sidebar.classList.remove("sidebar-open");
}

function showToast(message) {
    const toast = document.querySelector("#toast");
    toast.textContent = message;
    toast.classList.add("toast-visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("toast-visible"), 2800);
}

function openModal(action) {
    const messages = {
        "add-student": ["Add a student", "Student-only sign-in and secure account creation are planned. This preview does not create or store accounts yet."],
        "add-resource": ["Share a learning material", "Notes, videos and quizzes will be published to selected batches after secure uploads and classroom storage are connected."]
    };
    const [title, copy] = messages[action] ?? ["Coming in the next build", "This preview shows the planned workflow."];
    modalTitle.textContent = title;
    modalCopy.textContent = copy;
    modal.hidden = false;
    document.querySelector("#modal-ok").focus();
}

navLinks.forEach((link) => link.addEventListener("click", () => showView(link.dataset.view)));
document.querySelectorAll("[data-view-link]").forEach((link) => {
    link.addEventListener("click", () => showView(link.dataset.viewLink));
});
document.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", () => openModal(button.dataset.action));
});
document.querySelector("#modal-close").addEventListener("click", () => { modal.hidden = true; });
document.querySelector("#modal-ok").addEventListener("click", () => { modal.hidden = true; });
modal.addEventListener("click", (event) => {
    if (event.target === modal) modal.hidden = true;
});
document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") modal.hidden = true;
});
document.querySelector("#mobile-menu").addEventListener("click", () => {
    sidebar.classList.toggle("sidebar-open");
});
document.querySelector(".help-button").addEventListener("click", () => showToast("Help center will be available with the full app."));
document.querySelector(".notification-button").addEventListener("click", () => showToast("You’re all caught up."));
