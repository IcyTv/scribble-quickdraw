const { task, dest, src, series, parallel, watch } = require("gulp");
const sourcemaps = require("gulp-sourcemaps");
const sass = require("gulp-sass");
const dart = require("dart-sass");
const validator = require("gulp-w3c-html-validator");
const ejs = require("gulp-ejs");
const beautify = require("gulp-jsbeautifier");
const fileAssets = require("gulp-file-assets");
const debug = require("gulp-debug");
const browserSync = require("browser-sync").create();

const browserify = require("browserify");
const tsify = require("tsify");
const tap = require("gulp-tap");
const buffer = require("gulp-buffer");
const rename = require("gulp-rename");

const mode = require("gulp-mode")({
	modes: ["production", "development"],
	default: "development"
});

sass.compiler = dart;

const beautifyOpt = {
	indent_with_tabs: true,
	space_after_anon_function: true,
	space_after_named_function: true,
	keep_array_indentation: false
};

const brOpts = { cache: {}, packageCache: {}, debug: true };

task("ts", () => {
	return (
		src("app/client/assets/ts/*.ts")
			.pipe(debug())
			.pipe(
				tap(file => {
					file.contents = browserify(file.path, brOpts)
						.plugin(tsify)
						.bundle();
				})
			)
			.pipe(buffer())
			//.pipe(sourcemaps.init())
			// .pipe(
			// 	babel({
			// 		presets: ["@babel/preset-env"]
			// 	})
			// )
			//.pipe(uglify())
			//.pipe(sourcemaps.write())
			.pipe(
				rename({
					extname: ".js"
				})
			)
			.pipe(dest("dist/client/assets/js"))
	);
});

task("watchify", () => {});

task("css", () => {
	return src("app/client/assets/css/**.scss")
		.pipe(mode.development(sourcemaps.init()))
		.pipe(sass())
		.on("error", sass.logError)
		.pipe(mode.development(beautify(beautifyOpt)))
		.pipe(mode.development(sourcemaps.write()))
		.pipe(dest("dist/client/assets/css"));
});

task("html", () => {
	src("app/client/**.html")
		.pipe(
			fileAssets({
				excludes: ["html", "css", "js"]
			})
		)
		.pipe(dest("dist/client"));
	return (
		src("app/client/**.html")
			.pipe(
				ejs({
					root: "/app/client/assets/html",
					compileDebug: true
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
			baseDir: "./dist/client"
		},
		open: false
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
				baseDir: "dist/client"
			}
		});

		watch("./app/client", series("html", "reload"));
		watch("./app/client/assets/css", series("css", "reload"));
		watch("app/client/assets/ts", series("ts", "reload"));
	})
);
