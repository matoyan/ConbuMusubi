<?php
# 画像サイズの変換
# サイズが分かる場合（保留）：xsize, ysize
# サイズが分からない場合：maxsize
# 
# /tmp/yr_imgcache ディレクトリにキャッシュを作成。
# ファイルがなければ、元ファイルを変換してキャッシュ作成。
# 基本的に同じファイル名はつかないはずなので、キャッシュは作らない。
# 
# キャッシュファイルの命名規則
# maxsize_xxx_dir1-dir2-...dirx-filename.filetype
# 
# fname: xoops_homedirからのパス
# maxsize: 縦もしくは横の最大長

# 以下対応未。
# x_maxsize: 横の最大長
# y_maxsize: 縦の最大長
# xsize: 横長
# ysize: 縦長

error_reporting(E_ERROR);

$cachedir="/home2/mountrec/public_html/musubi/thumb/";

if(empty($_GET['fname'])){
	exit();
}
$fname = "/home2/mountrec/public_html/musubi/data/", $_GET['fname']);

if(!empty($_GET['maxheight'])){
	$maxheight=intval($_GET['maxheight']);
}else{
	$maxheight=0;
}
if($maxheight>0){
	$h="h_".$maxheight."_";
}else{
	$h="";
}

if(!empty($_GET['maxsize']) and intval($_GET['maxsize'])>0 and !empty($fname)){
	$maxsize=intval($_GET['maxsize']);
	$fname=$xoops_homedir.$fname;
	$cachename=$cachedir."maxsize_".$maxsize."_".$h.$_GET['fname'];
}else{
	exit();
}

if(is_file($cachename)){
	print_img($cachename);
	exit();
}
list($width, $height, $type, $attr) = getimagesize($fname);


// ファイル有無チェック
if(!is_file($fname)){
	exit();
}

// リサイズして保存
$ftype=strrchr($fname, "."); // 拡張子から

if(strcasecmp($ftype, ".png")==0){
	$src_id = imagecreatefrompng($fname);
}elseif(strcasecmp($ftype, ".jpg")==0 or strcasecmp($ftype, ".jpeg")==0){
	$src_id = imagecreatefromjpeg($fname);
}elseif(strcasecmp($ftype, ".gif")==0){
	$src_id = imagecreatefromgif($fname);
}

if($width >= $height){
	$th_width  = $maxsize;
	$th_height = $height * $th_width / $width;
} else {
	$th_height = $maxsize;
	$th_width  = $width * $th_height / $height;
}

if($maxheight>0){
	if($th_height>$maxheight){
		$th_width  = $th_width * $maxheight / $th_height;
		$th_height = $maxheight;
	}
}
$th_width=intval($th_width);
$th_height=intval($th_height);

$dst_id = imagecreatetruecolor($th_width, $th_height);
imagecopyresampled($dst_id, $src_id, 0, 0, 0, 0, $th_width, $th_height, $width, $height);

if(strcasecmp($ftype, ".png")==0){
	imagepng($dst_id, $cachename);
}elseif(strcasecmp($ftype, ".jpg")==0 or strcasecmp($ftype, ".jpeg")==0){
	imagejpeg($dst_id, $cachename, 100);
}elseif(strcasecmp($ftype, ".gif")==0){
	imagegif($dst_id, $cachename);
}

imagedestroy($src_id);
imagedestroy($dst_id);

print_img($cachename);
exit();


function print_img($imgname){
	$ftype=strrchr($imgname, "."); 
	$type="Content-type: image/";
	if(strcasecmp($ftype, ".png")==0){
		$type.="png\n\n";
	}elseif(strcasecmp($ftype, ".jpg")==0 or strcasecmp($ftype, ".jpeg")==0){
		$type.="jpeg\n\n";
	}elseif(strcasecmp($ftype, ".gif")==0){
		$type.="gif\n\n";
	}else{
		exit();
	}
	header($type);
	readfile($imgname);
}

?>
