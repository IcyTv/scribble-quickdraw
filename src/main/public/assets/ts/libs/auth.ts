import { parse } from "cookie";
import { Base64 } from "js-base64";

export const checkAuth = () => {
	try {
		let cookie = parse(document.cookie);
		if (cookie.Authorization == undefined) {
			throw new Error("User is not authenticated");
		}
	} catch (e) {
		window.location.href = "/index.html?error=" + Base64.encode(e);
	}
};
