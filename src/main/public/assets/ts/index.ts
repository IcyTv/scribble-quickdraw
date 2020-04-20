import { serialize } from "cookie";
import { Base64 } from "js-base64";
import jwt_decode from "jwt-decode";
import parser from "query-string";
import Url from "url-parse";

window.onload = () => {
	let login = document.getElementById("lg-form");
	let submit = login.querySelector("input[type=submit]");
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
		fetch("http://localhost:8080/api/users/login", {
			method: "POST",
			body: JSON.stringify({
				username: uname,
				password: pw,
			}),
		})
			.then((data) => {
				if (data.ok) {
					return data.json();
				} else if (data.status == 403) {
					throw Error("Username or password wrong");
				} else {
					throw Error("Something went wrong");
				}
			})
			.then((data) => {
				let jwt = jwt_decode(data.jwt);
				let c = serialize("Authorization", "Bearer " + data.jwt, {
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
};
