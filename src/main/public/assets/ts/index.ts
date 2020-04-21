// import { serialize } from "cookie";
import { Base64 } from "js-base64";
import parser from "query-string";
import Url from "url-parse";

window.onload = () => {
	let login = document.getElementById("lg-form");
	let submit = login.querySelector("input[name=start-btn]");
	let register = login.querySelector("input[name=register-btn]");
	let errorP = document.getElementById("err-box");

	let error: string = <string>(
		parser.parse(new Url(window.location.href)["query"]).error
	);
	if (error != undefined) {
		try {
			errorP.innerHTML = Base64.decode(error);
			errorP.style.color = "red";
			errorP.style.display = "block";
		} catch (e) {
			console.error(e);
		}
	}

	submit.addEventListener("click", (e) => {
		console.log("Submitted!");
		let uname = login["username"].value;
		let pw = login["password"].value;
		fetch("/api/users/login", {
			method: "POST",
			body: JSON.stringify({
				username: uname,
				password: pw,
			}),
		})
			.then((data) => {
				if (data.ok) {
					console.log("Logged in");
					errorP.innerHTML = "Logged in!";
					errorP.style.color = "green";
					errorP.style.display = "block";
					console.log(errorP);
					setTimeout(() => {
						window.location.href = "waiting.html";
						console.log("redirect");
					}, 3000);
				} else if (data.status == 403) {
					throw Error("Username or password wrong");
				} else {
					throw Error("Something went wrong");
				}
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
		console.log("redirecting to register-window");
		window.location.href = "register.html"; //redirect to next page?
		e.preventDefault();
	});
};
