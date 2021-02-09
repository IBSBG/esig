function checkAgentStatus() {
    let request = new XMLHttpRequest();
    request.open("GET", "http://localhost:9795/nexu-info");
    request.send();
    request.onload = () => {
        if (request.status == 200) {
            let responseText = JSON.parse(request.response).version;
            console.log(JSON.parse(request.response).version);
            if ("1.23-SNAPSHOT" === responseText) {
                document.getElementById("message").style.color = "green";
                document.getElementById("message").innerText = "Успешно свързване с NexU локална компонента!";
                document.getElementById("submitButton").disabled = false;
            }
        } else {
            alert("ERROR status=" + request.status + " statusText=" + request.statusText);
        }
    }
}

document.addEventListener('submit', function (event) {

    // Prevent form from submitting to the server
    event.preventDefault();

    let formData = new FormData(event.target);
    let file = formData.get('file');

    alert(" Файл [" + file.name + "]" +
        "\n Размер на файла [" + file.size + " bytes]" +
        "\n Контейнер [" + formData.get('container') + "]" +
        "\n Формат на подписа [" + formData.get('signatureFormat') + "]" +
        "\n Тип на подписа [" + formData.get('packagingFormat') + "]" +
        "\n Ниво на подписa [" + formData.get('signatureLevel') + "]" +
        "\n Хеш алгоритъм [" + formData.get('digestAlgorithm') + "]"
    );

    let reader = new FileReader();
    reader.onload = function () {
        let arrayBuffer = this.result;
        let array = new Uint8Array(arrayBuffer);
        let binaryString = String.fromCharCode.apply(null, array);
        alert("Файл byte array [" + array + "]");
        alert("Извикване на локална компонента за подписване с вече събраните данни!");
        // document.querySelector('#result').innerHTML = arrayBuffer + '  '+arrayBuffer.byteLength;
    }
    reader.readAsArrayBuffer(file);

});

function changeSignatureLevel(signatureFormat) {
    document.getElementById("signatureLevelBASELINE_B").checked  = true;
    document.getElementById("signatureLevelBASELINE_B").value = signatureFormat + "_BASELINE_B";
    document.getElementById("signatureLevelBASELINE_B_label").innerText = signatureFormat + "-BASELINE_B";

    document.getElementById("signatureLevelBASELINE_T").value = signatureFormat + "BASELINE_T";
    document.getElementById("signatureLevelBASELINE_T_label").innerText = signatureFormat + "-BASELINE_T";

    document.getElementById("signatureLevelBASELINE_LT").value = signatureFormat + "BASELINE_LT";
    document.getElementById("signatureLevelBASELINE_LT_label").innerText = signatureFormat + "-BASELINE_LT";

    document.getElementById("signatureLevelBASELINE_LTA").value = signatureFormat + "BASELINE_LTA";
    document.getElementById("signatureLevelBASELINE_LTA_label").innerText = signatureFormat + "-BASELINE_LTA";
}

function changeContainer(container) {

}