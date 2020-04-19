import cookie from "cookie";
import jwt_decode from "jwt-decode";
import p5 from "p5";
import * as svg from "p5.js-svg";
import parser from "query-string";
import Url from "url-parse";
import simplify from "simplify-js";

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
	buf: {coords: {x: number, y: number}[], index: number};
	sendInt: NodeJS.Timeout;
}

const s = (p: p5) => {
	let d: sketch = {
		width: innerWidth - innerWidth / 5,
		height: innerHeight,
		pic: [],
		buf: undefined,
		sendInt: null,
	};

	let send = () => {
		let index = d.buf.index;
		let coords = d.buf.coords;
		let simple = simplify(coords);
		console.log(coords);
		d.buf.coords = [];
		d.buf.index += 1;
		socket.send(JSON.stringify({
			coords: simple,
			index: index
		}))
	}

	p.setup = () => {
		let canvas = p.createCanvas(d.width, d.height, svg.SVG);
		canvas.parent("p5-sketch");
		//d.sendInt = setInterval(send, 100);
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

	p.touchStarted = () => {
		d.pic.push([]);
		d.sendInt = setInterval(send, 100);
	};

	p.touchMoved = (ev: MouseEvent) => {
		let last = d.pic.length - 1;
		d.pic[last].push({
			x: p.constrain(ev.offsetX, 0, p.width),
			y: p.constrain(ev.offsetY, 0, p.height),
		});
	};

	p.touchEnded = () => {
		let last = d.pic.length - 1;
		clearInterval(d.sendInt);
		d.buf.index = 0;
	};

	socket.onmessage = (ev) => {
		//console.log(ev.data);
		let json = JSON.parse(ev.data);
		if (json.currentPlayer != player) {
			console.log(json.currentPlayer);
			if(json.append) {
				d.pic[d.pic.length - 1].concat(json.data);
			} else {
				d.pic.push(json.data);
			}
		}
	};
};
