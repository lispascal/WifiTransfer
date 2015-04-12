function rename(element, dir) {
   oldName = element.parentElement.getAttribute("name");
   var result = prompt("New File Name?", oldName);

// if they didn't hit cancel and name is different, create form then submit it
   if(result != null && result != oldName)
   {
       var form = document.createElement("form");
       form.setAttribute("method", "POST");
// path
       form.setAttribute("action", "rename.html?path=" + dir);
       form.setAttribute("accept-charset", "UTF-8");
       form.setAttribute("enctype", "multipart/form-data");

// first argument in post
       var inp1 = document.createElement("input");
       inp1.setAttribute("type", "hidden");
       inp1.setAttribute("name", "oldName");
       inp1.setAttribute("value", oldName);
       form.appendChild(inp1);
// second argument in post
       var inp2 = document.createElement("input");
       inp2.setAttribute("type", "hidden");
       inp2.setAttribute("name", "newName");
       inp2.setAttribute("value", result);
       form.appendChild(inp2);
// submit form
       document.body.appendChild(form);
       form.submit();
    }
}

var layout = "list";
function changeLayout() {
	removeOldPopup(); // if changing layout remove old iconPopup. saves a check

	var newLayout = (layout=="list") ? "icon" : "list";
    var list = document.getElementsByClassName(layout);
    for(var i = list.length-1; i>=0; --i) {
        var oldClass = list[i].getAttribute("class");
        var newClass = oldClass.replace(layout, newLayout);
        list[i].setAttribute("class", newClass);
    }
    layout = newLayout;
}


function removeOldPopup() {
	var div = document.getElementById("iconPopup");
	if(div)
		div.remove();
}

function popupStuff(x, y, target) {
	var div = document.createElement("div");
	div.id = "iconPopup";
	for(var i = 0; i < 3; i++)
		div.appendChild(target.children[i].cloneNode(true));
	div.style.top = y;
	div.style.left = x;
	div.setAttribute("name", target.getAttribute("name"));
	document.body.appendChild(div);
}

document.addEventListener('contextmenu', function(e) {
	console.log(e);
	removeOldPopup();
	if(e.target.hasAttribute("class") && e.target.getAttribute("class").indexOf("icon") >= 0 && e.target.getAttribute("class").indexOf("file") >= 0) // right clicked an icon
	{
		console.log("inner part!");
		popupStuff(e.pageX, e.pageY, e.target);
		e.preventDefault();
	}
}, false);

var wasAscending = false;
function sortByName() {
    var objList = [];

    var arr = document.getElementsByClassName("file");
    for(var i = 0; i < arr.length; i++) // add all elements to the array with a name value
    {
        var element = arr[i];
        var name = element.getElementsByTagName("span")[0].innerHTML;
        objList.push({"name":name, "ref":element});
    }
    var arr2 = document.getElementsByClassName("dir");
    for(var i = 0; i < arr2.length; i++)
    {
        var element = arr2[i];
        var name = element.getElementsByTagName("span")[0].innerHTML;
        if(name.toLowerCase() !== ("parent folder")) // leave parent folder at top
            objList.push({"name":name, "ref":element});
    }

    // sort elements by name
    if(!wasAscending)
        objList.sort(function(a, b) {
            return a.name.localeCompare(b.name); // ascending order = smallest to largest
        });
    else
        objList.sort(function(a, b) {
            return b.name.localeCompare(a.name); // descending order = largest to smallest
        });

    wasAscending = !wasAscending;

    var parent = document.getElementById("directoryContainer");

    // remove elements from parent
    for(var i in objList)
        parent.removeChild(objList[i].ref);

    // place elements back in parent
    for(var i in objList)
        parent.appendChild(objList[i].ref);

}

function sortBySize() {
    var objList = [];

    var arr = document.getElementsByClassName("file");
    for(var i = 0; i < arr.length; i++) // add all elements to the array with a size value
    {
        var element = arr[i];
        var size = element.getElementsByTagName("span")[1].dataset.size;
        objList.push({"size":size, "ref":element});
    }

    // sort elements by size
    if(!wasAscending)
        objList.sort(function(a, b) {
            return a.size - b.size; // ascending order = smallest to largest
        });
    else
        objList.sort(function(a, b) {
            return b.size - a.size; // descending order = largest to smallest
        });

    wasAscending = !wasAscending;

    var parent = document.getElementById("directoryContainer");

    // remove elements from parent
    for(var i in objList)
        parent.removeChild(objList[i].ref);

    // place elements back in parent
    for(var i in objList)
        parent.appendChild(objList[i].ref);

}


function sortByDate() {
    var objList = [];

    var arr = document.getElementsByClassName("file");
    for(var i = 0; i < arr.length; i++) // add all elements to the array with a time value
    {
        var element = arr[i];
        var time = element.getElementsByTagName("span")[2].dataset.modified;
        objList.push({"time":time, "ref":element});
    }

    // sort elements by time
    if(!wasAscending)
        objList.sort(function(a, b) {
            return a.time - b.time; // ascending order = newest to oldest
        });
    else
        objList.sort(function(a, b) {
            return b.time - a.time; // descending order = oldest to newest
        });

    wasAscending = !wasAscending;

    var parent = document.getElementById("directoryContainer");

    // remove elements from parent
    for(var i in objList)
        parent.removeChild(objList[i].ref);

    // place elements back in parent
    for(var i in objList)
        parent.appendChild(objList[i].ref);

}