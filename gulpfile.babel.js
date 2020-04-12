import browserSync from "browser-sync";
import dart from "dart-sass";
import { dest, parallel, series, src, task, watch } from "gulp";
import buffer from "gulp-buffer";
import debug from "gulp-debug";
import ejs from "gulp-ejs";
import fileAssets from "gulp-file-assets";
import gulpif from "gulp-if";
import beautify from "gulp-jsbeautifier";
import rename from "gulp-rename";
import sass from "gulp-sass";
import sourcemaps from "gulp-sourcemaps";
import validator from "gulp-w3c-html-validator";
import reduce from "gulp-watchify-factor-bundle";
import tsify from "tsify";
import yargs from "yargs";

const argv = yargs.options({
	browser: { type: "boolean", default: false },
	out: { type: "string", default: "src/test/public" },
}).argv;

const mode = require("gulp-mode")({
	modes: ["production", "development"],
	default: "development",
});

const srcDir = "src/main/public";
const outDir = argv.out;

sass.compiler = dart;

const beautifyOpt = {
	indent_with_tabs: true,
	space_after_anon_function: true,
	space_after_named_function: true,
	keep_array_indentation: false,
};

task("ts", () => {
	const basedir = srcDir + "/assets/ts";
	const b = reduce.create({
		basedir: basedir,
		debug: true,
		builtins: [],
	});
	b.plugin(tsify);
	return reduce
		.src(srcDir + "/assets/ts/*.ts")
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
		.pipe(reduce.dest(outDir + "/assets/js"));
});

task("css", () => {
	return src(srcDir + "/assets/css/**.scss")
		.pipe(mode.development(sourcemaps.init()))
		.pipe(sass())
		.on("error", sass.logError)
		.pipe(mode.development(beautify(beautifyOpt)))
		.pipe(mode.development(sourcemaps.write()))
		.pipe(dest(outDir + "/assets/css"));
});

task("html", (cb) => {
	src(srcDir + "/**.html")
		.pipe(
			fileAssets({
				excludes: ["html", "css", "js"],
			})
		)
		.pipe(dest(outDir));
	return src(srcDir + "/**.html")
		.pipe(
			ejs({
				root: srcDir + "/assets/html",
				compileDebug: true,
			})
		)
		.pipe(mode.development(beautify(beautifyOpt)))
		.pipe(validator())
		.pipe(dest(outDir));
});

task("browser", () => {
	browserSync.init({
		server: {
			baseDir: outDir,
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
	series("default", (cb) => {
		if (argv.browser) {
			browserSync.init({
				server: {
					baseDir: outDir,
				},
			});
		}
		let html = watch(srcDir + "/**/*.html", parallel("html"));
		html.on("change", (ev) => {
			if (argv.browser) {
				browserSync.notify("Compiling, please wait!");
				series("html");
				browserSync.reload("*.html");
			} else {
				parallel("html");
			}
		});
		let css = watch(srcDir + "/assets/css/**/*.scss", parallel("css"));
		css.on("change", (ev) => {
			if (argv.browser) {
				browserSync.notify("Compiling, please wait!");
				series("css");
				browserSync.reload("*.css");
			} else {
				parallel("css");
			}
		});
		let basedir = srcDir + "/assets/ts";

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
			.src(srcDir + "/assets/ts/*.ts")
			// apply `factor-bundle` and `watchify`
			.pipe(reduce.watch(b, { common: "common.js" }, { debug: true }))
			// whenever `b.bundle()` is called,
			// event 'bundle' is fired
			.on("bundle", function (vinylStream) {
				if (argv.browser) {
					browserSync.notify("Compiling, please wait!");
				}
				vinylStream
					// same with gulp.dest
					.pipe(buffer())
					.pipe(
						rename({
							extname: ".js",
						})
					)
					.pipe(reduce.dest(outDir + "/assets/js", {}))
					.pipe(gulpif(argv.browser, browserSync.stream()));
			});
		cb();
	})
);
