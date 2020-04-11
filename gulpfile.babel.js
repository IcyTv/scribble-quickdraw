import browserSync from "browser-sync";
import dart from "dart-sass";
import { dest, parallel, series, src, task, watch } from "gulp";
import buffer from "gulp-buffer";
import debug from "gulp-debug";
import ejs from "gulp-ejs";
import fileAssets from "gulp-file-assets";
import beautify from "gulp-jsbeautifier";
import rename from "gulp-rename";
import sass from "gulp-sass";
import sourcemaps from "gulp-sourcemaps";
import validator from "gulp-w3c-html-validator";
import reduce from "gulp-watchify-factor-bundle";
import tsify from "tsify";

const mode = require("gulp-mode")({
	modes: ["production", "development"],
	default: "development",
});

sass.compiler = dart;

const beautifyOpt = {
	indent_with_tabs: true,
	space_after_anon_function: true,
	space_after_named_function: true,
	keep_array_indentation: false,
};

task("ts", () => {
	const basedir = "app/client/assets/ts";
	const b = reduce.create({
		basedir: basedir,
		debug: true,
		builtins: [],
	});
	b.plugin(tsify);
	return reduce
		.src("app/client/assets/ts/*.ts")
		.pipe(
			reduce.bundle(b, {
				common: "common.js",
			})
		)
		.pipe(debug())
		.pipe(buffer())
		.pipe(
			rename({
				extname: ".js",
			})
		)
		.pipe(reduce.dest("dist/client/assets/js"));
});

task("css", () => {
	return src("app/client/assets/css/**.scss")
		.pipe(mode.development(sourcemaps.init()))
		.pipe(sass())
		.on("error", sass.logError)
		.pipe(mode.development(beautify(beautifyOpt)))
		.pipe(mode.development(sourcemaps.write()))
		.pipe(dest("dist/client/assets/css"));
});

task("html", (cb) => {
	src("app/client/**.html")
		.pipe(
			fileAssets({
				excludes: ["html", "css", "js"],
			})
		)
		.pipe(dest("dist/client"));
	return (
		src("app/client/**.html")
			.pipe(
				ejs({
					root: "/app/client/assets/html",
					compileDebug: true,
				})
			)
			.pipe(mode.development(beautify(beautifyOpt)))
			//.pipe(cat())
			.pipe(validator())
			.pipe(dest("dist/client"))
	);
});

task("browser", () => {
	browserSync.init({
		server: {
			baseDir: "./dist/client",
		},
		open: false,
	});
});

task("reload", () => {
	browserSync.reload();
});

task("default", parallel("ts", "css", "html"));

task(
	"watch",
	series("default", () => {
		browserSync.init({
			server: {
				baseDir: "dist/client",
			},
		});
		let html = watch("./app/client/**/*.html", parallel("html"));
		html.on("change", (ev) => {
			console.log(ev);
			browserSync.notify("Compiling, please wait!");
			series("html");
			browserSync.reload("*.html");
		});
		let css = watch("./app/client/assets/css/**/*.scss", parallel("css"));
		css.on("change", (ev) => {
			console.log(ev);
			browserSync.notify("Compiling, please wait!");
			series("css");
			browserSync.reload("*.css");
		});
		let basedir = "app/client/assets/ts";

		// Create a browserify instance
		// same with `browserify(opts)`
		let b = reduce.create({
			basedir: basedir,
			debug: true,
			builtins: [],
		});

		b.on("log", console.log.bind(console));
		b.plugin(tsify);

		// find entries
		// same with gulp.src()

		reduce
			.src("app/client/assets/ts/*.ts")
			// apply `factor-bundle` and `watchify`
			.pipe(reduce.watch(b, { common: "common.js" }, { debug: true }))
			// whenever `b.bundle()` is called,
			// event 'bundle' is fired
			.on("bundle", function (vinylStream) {
				browserSync.notify("Compiling, please wait!");
				vinylStream
					// same with gulp.dest
					.pipe(buffer())
					.pipe(
						rename({
							extname: ".js",
						})
					)
					.pipe(reduce.dest("dist/client/assets/js", {}))
					.pipe(browserSync.stream());
			});
		// let ts = watch("app/client/assets/ts/**/*.ts");
		// ts.on("change", (ev) => {
		// 	console.log(ev);
		// 	series("ts");
		// 	browserSync.notify("Compiling, please wait!");
		// 	browserSync.reload("*.js");
		// });
	})
);
