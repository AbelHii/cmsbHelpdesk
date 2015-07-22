<?php
$id = $_POST["id"];

$target_path  = "../../images/$id/";

//create new directory named id if it doesn't exist
if(!file_exists($target_path)){
	mkdir($target_path, 0777, true);
	chmod($target_path, 0777);
}

$file = $_FILES['uploadedfile']['tmp_name'];
$filename = basename($_FILES['uploadedfile']['name']);

//to remove whitespace and periods and commas from the picture name
//so that the URL will be valid:
//the loop is so that it doesn't delete the last period which is needed for the file type
$num = 0;
for($i = 0; $i <= strlen($filename); $i++){
	if($filename{$i} == "." || $filename{$i} == ","){
		++$num;
	}
}
$filename = preg_replace('~[.,]~', '', $filename, $num-1);
$filename = preg_replace('~\s+~', '', $filename);


$target_path = $target_path . $filename;
$save = $file;
//to compress the image
if (isset ($_FILES['uploadedfile'])){
		$save = $file; //This is the new file you're saving

		list($width, $height) = getimagesize($file) ;

		if($width > 700 || $height > 700){
			$modwidth = $width*0.5; 
			$modheight = $height*0.5; 
		}else{
			$modwidth = $width; 
			$modheight = $height; 
		}	

		$tn = imagecreatetruecolor($modwidth, $modheight) ; 
		$image = imageCreateFromAny($save) ; 
		imagecopyresampled($tn, $image, 0, 0, 0, 0, $modwidth, $modheight, $width, $height) ; 

		$tn = getOrientation($tn, $save);
		imagejpeg($tn, $save, 60) ;
}	


if(move_uploaded_file($save, $target_path)) 
{
    echo "The file ". $filename. " has been uploaded";
} 
else
{
    echo "There was an error uploading the file, please try again!";
}


//to store image for any format:
function imageCreateFromAny($filepath) {
    $type = exif_imagetype($filepath); // [] if you don't have exif you could use getImageSize()
    $allowedTypes = array(
        1,  // [] gif
        2,  // [] jpg
        3,  // [] png
        6   // [] bmp
    );
    if (!in_array($type, $allowedTypes)) {
        return false;
    }
    switch ($type) {
        case 1 :
            $im = imageCreateFromGif($filepath) ;
        break;
        case 2 :
            $im = imageCreateFromJpeg($filepath);
        break;
        case 3 :
            $im = imageCreateFromPng($filepath) ;
        break;
        case 6 :
            $im = imageCreateFromBmp($filepath) ;
        break;
    }   

    return $im;//getOrientation($im, $filepath); 
}

function getOrientation($image, $filepath){
	//To get correct orientation:
	$exif = exif_read_data($filepath);
	if(!empty($exif['Orientation'])) {
	    switch($exif['Orientation']) {
	        case 3:
	            $image = imagerotate($image,180,0);
	            break;
	        case 6:
	            $image = imagerotate($image,-90,0);
	            break;
	        case 8:
	            $image = imagerotate($image,90,0);
	            break;
	    }
	}

	return $image;
}

?>;