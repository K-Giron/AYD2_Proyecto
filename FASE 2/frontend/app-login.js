"use strict";

function goToRoleHome(role) {
    location.href = sgoLandingPageFor(role);
}

// Si ya hay una sesión activa, no pedir login de nuevo.
const existingSession = sgoGetSession();
if (existingSession) {
    goToRoleHome(existingSession.role);
}

let selectedRole = "CLIENTE";

document.querySelectorAll(".auth-role-btn").forEach((button) => {
    button.addEventListener("click", () => {
        selectedRole = button.dataset.role;
        document.querySelectorAll(".auth-role-btn").forEach((b) => {
            b.classList.toggle("active", b === button);
            b.setAttribute("aria-selected", String(b === button));
        });
        document.getElementById("login-error").classList.add("hidden");
        // Solo los clientes se registran; el personal interno usa cuentas asignadas.
        document.getElementById("register-alternative").classList.toggle("hidden", selectedRole !== "CLIENTE");
    });
});

document.getElementById("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = document.getElementById("login-email").value;
    const password = document.getElementById("login-password").value;
    const remember = document.getElementById("login-remember").checked;
    const errorEl = document.getElementById("login-error");
    const submitButton = event.target.querySelector("button[type=submit]");

    errorEl.classList.add("hidden");
    if (submitButton) submitButton.disabled = true;
    const result = await sgoLogin(email, password);
    if (submitButton) submitButton.disabled = false;

    if (!result.ok) {
        errorEl.textContent = result.message;
        errorEl.classList.remove("hidden");
        return;
    }
    const selectedInternal = selectedRole === "STAFF";
    const accountIsInternal = ["STAFF", "FINANCE", "SAT_AUDITOR", "ONLINE_STORE"].includes(result.session.role);
    if ((selectedInternal && !accountIsInternal) || (!selectedInternal && result.session.role !== "CLIENTE")) {
        const otherLabel = result.session.role === "CLIENTE" ? "Cliente" : "Personal interno";
        errorEl.textContent = `Esa cuenta pertenece al perfil "${otherLabel}". Cambia la pestaña de arriba.`;
        errorEl.classList.remove("hidden");
        return;
    }

    sgoCreateSession(result.session, remember);
    goToRoleHome(result.session.role);
});

document.getElementById("reveal-demo-btn").addEventListener("click", () => {
    document.getElementById("demo-credentials").classList.toggle("hidden");
});
