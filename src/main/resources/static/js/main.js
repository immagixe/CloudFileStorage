$('#displayName').keypress(function (e) {
    var txt = String.fromCharCode(e.which);
    if (!txt.match(/[A-Za-z0-9&. ]/)) {
        return false;
    }
});

$('#displayName').bind('paste', function() {
    setTimeout(function() {
        var value = $('#displayName').val();
        var updated = value.replace(/[^A-Za-z0-9&. ]/g, '');
        $('#displayName').val(updated);
    });
});