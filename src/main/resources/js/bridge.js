let ws = new WebSocket("wss://localhost:30385/octo/userJoin");

let peerConnectionConfig = {
    'iceServers': [
        {urls: 'stun:123.56.108.66:41640'}
    ]
};

let constraints = {
    audio: true,
    video: true
};

let peer = new RTCPeerConnection(peerConnectionConfig);
let local_video;
let remote_video;

window.onload = function () {
    local_video = document.getElementById('local_v');
    remote_video = document.getElementById('remote_v');
};

let localStream;

function sendMessage(m) {
    console.log("sendMessage:", m)
}

function gotDescription(description) {
    console.log('got description');
    peer.setLocalDescription(description, function () {
        sendMessage(JSON.stringify({'sdp': description}));
    }, function () {
        console.log('set description error')
    });
}

function gotIceCandidate(event) {
    if (event.candidate != null) {
        sendMessage(JSON.stringify({'ice': event.candidate}));
    }
}

function gotRemoteStream(event) {
    console.log('got remote stream');
    remote_video.srcObject = event.stream;
}

function createOfferError(error) {
    console.log(error);
}

peer.onicecandidate = gotIceCandidate;
peer.onaddStream = gotRemoteStream;

function gotStream(stream) {
    localStream = stream;
    peer.addStream(stream);
    peer.createOffer(gotDescription, createOfferError);
    local_video.srcObject = stream;
}

function logError(error) {
    console.error(error)
}

function start() {
    navigator.getUserMedia(constraints, gotStream, logError);
}

function stop() {
    localStream.getTracks().forEach(function (track) {
        track.stop();
    });
}