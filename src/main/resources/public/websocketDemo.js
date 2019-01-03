// small helper function for selecting element by id
let id = id => document.getElementById(id);

//Establish the WebSocket connection and set up event handlers
let ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/chat");
ws.onmessage = msg => updateChat(msg);
ws.onclose = () => alert("WebSocket connection closed");

// Add event listeners to button and input field
id("send").addEventListener("click", () => sendAndClear(id("message").value));
id("message").addEventListener("keypress", function (e) {
    if (e.keyCode === 13) { // Send message if enter is pressed in input field
        sendAndClear(e.target.value);
    }
});

function isJsonString(str)
{
    try
    {
        JSON.parse(str);
    }
    catch (e)
    {
        return false;
    }
    return true;
}

function sendAndClear(message) {
    if (message !== "") {
        ws.send(message);
        id("message").value = "";
    }
}

function updateChat(msg) { // Update chat-panel and list of connected users
    console.log('msg -> ' + msg.data);

    if(isJsonString(msg.data))
    {
        let data = JSON.parse(msg.data);
        id("chat_panel").value += data.map(item => item.message).join('\n') + '\n';
//        id("chat_panel").value += data[0].message + '\n';
        var chatPanel = id("chat_panel");
        chatPanel.scrollTop = chatPanel.scrollHeight;
    }
    else
    {
        id("chat_panel").value += msg.data + '\n';
        var chatPanel = id("chat_panel");
        chatPanel.scrollTop = chatPanel.scrollHeight;
    }


}
