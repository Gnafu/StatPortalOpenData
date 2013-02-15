<?php 
if(isset($variables['is_admin'])) {

	if($variables['is_admin']) {
		?>
		
		<div class="storage"><?php echo t('Banca dati') . ':<span id="storage-name">' . $storage . '</span>'; ?></div>
		
		<table class="services_status">
		<?php 
		
		foreach ($services as $name => $status) : ?>
			<tr>
				<td>
				<?php echo $name; ?>
				</td>
				<td>
				<?php echo $status['status_text']; ?>
				</td>
				<td>
				<?php 
					theme_image(array('path' => 'public://img_ok', 'attributes' => array()));
				?>
				</td>
			</tr>
		<?php 
		endforeach;  
		?>
		</table>
		<?php 
		
	}
	else 
		echo t('Non sei autorizzato ad accedere a questa risorsa');
}


?>

