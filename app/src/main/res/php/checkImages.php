<?php
$id = $_POST["id"];

$response = array();
$image_paths = array(); 
$count = 0;

$target_path  = "../../images/$id/";
if(file_exists($target_path)){
	$response["image_paths"] = array();
    $images = glob($target_path . "*.{jpg,png,gif}", GLOB_BRACE);

    foreach($images as $path){
    	$image_paths[$count] = $path;
    	$count++;
    }
    if($count == 0){
        $response["success"] = 0;
    }else{
        $response["success"] = 1;
    }
    $response["image_paths"] = $image_paths;
}else{
    $response["success"] = 0;
}

echo json_encode($response);
?>;