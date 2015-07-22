<?php
    $filename = $_POST["delete_file"];
    $id = $_POST["id"];
    $filename = "../../images/$id/".basename($filename);
    
    if (file_exists($filename)) {
        unlink($filename);
        echo 'File '.$filename.' has been deleted';
    } else {
        echo 'Could not delete '.$filename.', file does not exist';
    }
?>