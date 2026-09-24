"use strict";

function goToRoleHome(role) {
    location.href = sgoLandingPageFor(role);
}

// Si ya hay una sesión simulada activa, no pedir login de nuevo.
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
    });
});

document.getElementById("login-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const email = document.getElementById("login-email").value;
    const password = document.getElementById("login-password").value;
    const remember = document.getElementById("login-remember").checked;
    const errorEl = document.getElementById("login-error");

    const account = sgoFindAccount(email, password);
    if (!account) {
        errorEl.textContent = "Correo o contraseña incorrectos.";
        errorEl.classList.remove("hidden");
        return;
    }
    const selectedInternal = selectedRole === "STAFF";
    const accountIsInternal = ["STAFF", "FINANCE", "SAT_AUDITOR"].includes(account.role);
    if ((selectedInternal && !accountIsInternal) || (!selectedInternal && account.role !== "CLIENTE")) {
        const otherLabel = account.role === "CLIENTE" ? "Cliente" : "Personal interno";
        errorEl.textContent = `Esa cuenta pertenece al perfil "${otherLabel}". Cambia la pestaña de arriba.`;
        errorEl.classList.remove("hidden");
        return;
    }

    errorEl.classList.add("hidden");
    sgoCreateSession(account, remember);
    goToRoleHome(account.role);
});

document.getElementById("reveal-demo-btn").addEventListener("click", () => {
    document.getElementById("demo-credentials").classList.toggle("hidden");
});
