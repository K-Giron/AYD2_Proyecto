"use strict";

document.getElementById("registration-form").addEventListener("submit", event => {
    event.preventDefault();
    const error = document.getElementById("registration-error");
    try {
        const account = sgoRegisterClient(
            document.getElementById("registration-name").value,
            document.getElementById("registration-email").value,
            document.getElementById("registration-password").value
        );
        sgoCreateSession(account, true);
        location.href = "cliente.html";
    } catch (failure) {
        error.textContent = failure.message;
        error.classList.remove("hidden");
    }
});
