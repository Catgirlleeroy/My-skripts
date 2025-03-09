document.addEventListener("DOMContentLoaded", function () {
    const loginBtn = document.getElementById("loginBtn");

    loginBtn.addEventListener("click", function () {
        const email = document.getElementById("email");
        const password = document.getElementById("password");
        email.style.border = "1px solid #ccc";
        password.style.border = "1px solid #ccc";

        if (email.value.trim() === "" || password.value.trim() === "") {
            alert("Please fill in both fields!");
            if (email.value.trim() === "") email.style.border = "2px solid red";
            if (password.value.trim() === "") password.style.border = "2px solid red";
            return;
        }
        function toggleDarkMode() {
            document.body.classList.toggle("dark-mode");
        }
        window.location.href = "HTMLtesting.html";
        const bgToggle = document.getElementById("bgToggle");
        function toggleDarkMode() {
            document.body.classList.toggle("dark-mode");
            localStorage.setItem("darkMode", document.body.classList.contains("dark-mode") ? "enabled" : "disabled");
        }
        if (localStorage.getItem("darkMode") === "enabled") {
            document.body.classList.add("dark-mode");
            bgToggle.checked = true;
        }
        bgToggle.addEventListener("change", toggleDarkMode);
    });
});