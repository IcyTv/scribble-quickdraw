import { serialize } from "cookie";
import jwt_decode from "jwt-decode";

window.onload = () => {
	console.log("tester-moin");
	let login = document.getElementById("lg-form");
	let submit = login.querySelector("input[name=start-btn]");
	let register = login.querySelector("input[name=register-btn]");
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
				let c = serialize("Authorization", "Bearer " + data, {
					expires: new Date(jwt["exp"] * 1000), //Because ms to s
					path: "/",
				});
				document.cookie = c;
				errorP.innerHTML = "Logged in!";
				errorP.style.color = "green";
				errorP.style.display = "block";
				//DO A REDIRECT
			})
			.catch((err) => {
				console.error(err);
				errorP.innerHTML = err;
				errorP.style.display = "block";
				errorP.style.color = "red";
			});
		e.preventDefault();
	});

	register.addEventListener("click", (e) => {
		console.log("redirecting to register-window")
		window.location.href = "https://www.IcyTv.de/prijects/scribble-quickdraw/register.html";	//redirect to next page?
	});
};
