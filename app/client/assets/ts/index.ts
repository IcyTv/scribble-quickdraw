import * as cookie from "cookie";
import * as jwt_decode from "jwt-decode";

window.onload = () => {
	let login = document.getElementById("lg-form");
	let submit = login.querySelector("input[type=submit]");
	let errorP = document.getElementById("err-box");

	submit.addEventListener("click", (e) => {
		console.log("Submitted!");
		let uname = login["username"].value;
		let pw = login["password"].value;
		fetch("http://localhost:8080/users/login", {
			method: "POST",
			body: JSON.stringify({
				username: uname,
				password: pw,
			}),
		})
			.then((data) => {
				if (data.ok) {
					return data.text();
				} else if (data.status == 403) {
					throw Error("Username or password wrong");
				} else {
					throw Error("Something went wrong");
				}
			})
			.then((data) => {
				let jwt = jwt_decode(data);
				let c = cookie.serialize("jwt", data, {
					expires: new Date(jwt.exp * 1000), //Because ms to s
					path: "/",
				});
				document.cookie = c;
				errorP.innerHTML = "Logged in!";
				errorP.style.color = "green";
				errorP.style.display = "block";
				//DO A REDIRECT
			})
			.catch((err) => {
				errorP.innerHTML = err;
				errorP.style.display = "block";
				errorP.style.color = "red";
			});
		e.preventDefault();
	});
};
