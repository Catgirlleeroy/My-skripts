document.addEventListener("DOMContentLoaded", function () {
    const loginBtn = document.getElementById("loginBtn");

    loginBtn.addEventListener("click", function () {
        const email = document.getElementById("email");
        const password = document.getElementById("password");

        // Reset previous errors
        email.style.border = "1px solid #ccc";
        password.style.border = "1px solid #ccc";

        if (email.value.trim() === "" || password.value.trim() === "") {
            alert("Please fill in both fields!");
            if (email.value.trim() === "") email.style.border = "2px solid red";
            if (password.value.trim() === "") password.style.border = "2px solid red";
            return;
        }

        // Redirect after successful validation
        window.location.href = "HTMLtesting.html"; // Change this to your destination page
    });
});
