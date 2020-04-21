window.onload = () => {
    console.log("another another 'tester-moin'");
	let waiting = document.getElementById("waiting-form");
    let logout = waiting.querySelector("input[name=logout-btn]");
    
    logout.addEventListener("click", (e) => {
        console.log("Submitted!");
        
        window.location.href = "https://www.IcyTv.de/prijects/scribble-quickdraw/index"; //redirect to next page?
    });
};