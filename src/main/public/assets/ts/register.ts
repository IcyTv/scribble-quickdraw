window.onload = () => {
    console.log("another 'tester-moin'");
	let register = document.getElementById("register-form");
    let submit = register.querySelector("input[name=start-btn]");
    
    submit.addEventListener("click", (e) => {
        console.log("Submitted!");
		let uname = register["username"].value;
        let pw = register["password"].value;
        let pwRepeat = register["passwordRepeat"].value;

        if(uname.length>0) {
            if(pw == pwRepeat){ 
                //add user content?
                console.log("because of not knowing what to do here your data has not been saved lol :P");  
                window.location.href = "https://www.IcyTv.de/prijects/scribble-quickdraw/waiting";  //redirect to next page?
            } else {
                throw Error("passwords do not match");
            }
        }else if(uname.*exists*) {  //request saved usernames?
            throw Error("this username has already been taken");
        }
    });
};