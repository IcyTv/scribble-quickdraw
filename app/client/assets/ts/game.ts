import cookie from "cookie";
import Url from "url-parse";
import parser from "query-string";
import p5 from "p5";
import jwt_decode from "jwt-decode";

let socket: WebSocket;
let player;

window.onload = () => {
	let jwt: string = cookie
		.parse(document.cookie)
		.Authorization.replace("Bearer ", "");

	console.log(parser.parse(new Url(window.location.href)["query"]));
	let room: number = +parser.parse(new Url(window.location.href)["query"])
		.room;
	socket = new WebSocket(
		"ws://localhost:8080/socket/" + room + "?jwt=" + jwt
	);
	socket.onopen = (ev) => {
		console.log(ev);
		console.log("Connected");
	};

	let jwtdec = jwt_decode(jwt);
	player = jwtdec["sub"];

	new p5(s);
};

// window.onbeforeunload = function () {
// 	console.log("closing");
// 	socket.onclose = function () {}; // disable onclose handler first
// 	socket.close();
// };

interface sketch {
	width: number;
	height: number;
	pic: { x: number; y: number }[][];
}

const s = (p: p5) => {
	let d: sketch = {
		width: innerWidth - innerWidth / 5,
		height: innerHeight,
		pic: [],
	};

	p.setup = () => {
		let canvas = p.createCanvas(d.width, d.height);
		canvas.parent("p5-sketch");
	};

	p.draw = () => {
		p.background(0);
		p.stroke(255);
		p.strokeWeight(3);
		p.noFill();

		for (let t of d.pic) {
			p.beginShape();
			for (let s of t) {
				p.vertex(s.x, s.y);
			}
			p.endShape();
		}
	};

	p.mousePressed = () => {
		d.pic.push([]);
	};

	p.mouseDragged = (ev: any) => {
		let last = d.pic.length - 1;
		if (p.mouseIsPressed) {
			d.pic[last].push({
				x: ev.offsetX,
				y: ev.offsetY,
			});
		}
	};

	p.mouseReleased = () => {
		let last = d.pic.length - 1;
		socket.send(
			JSON.stringify({
				data: d.pic[last],
			})
		);
	};

	socket.onmessage = (ev) => {
		console.log(ev.data);
		let json = JSON.parse(ev.data);
		if (json.currentPlayer != player) {
			d.pic.push(json.data);
		}
	};
};
