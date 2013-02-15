<?php
if(!isset($config))
	return;
?>
<div class="barousel_image">
<!-- images -->
<?php 
	$img_array = array();
	$title_array = array();
    $content_array = array();
        
    for($i=0;$i<$config['items_count']; $i++) {
    	$img = file_load($config['items'][$i]['image']);
        	
    	$img_array[] = file_create_url($img->uri);
    	$title_array[] = $config['items'][$i]['title'];
    	$content_array[] = $config['items'][$i]['news'];
    }
    
    $img_class = 'class="default"';
	foreach ($img_array as $key => $path) {
		$img_path = file_create_url($path);
		$url = url($img_path); 	
		        
		print '<img alt="barousel-image-' . $key . '" src="' . $url . '" ' . $img_class . '/>' ;
		$img_class = '';
	}
?>
</div>

<div class="barousel_content">
	<?php for($i=0;$i<$config['items_count']; $i++) : ?>
		<div <?php echo ($i==0 ? 'class="default"' : '');?> >
			<p class="header"> <?php echo $title_array[$i]; ?> </p>
			<?php echo $content_array[$i]; ?>
		</div>	
	<?php endfor; ?>
</div>
<div class="barousel_nav">

</div>