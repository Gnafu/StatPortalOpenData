<?php
/**
 * @file
 * Custom metadata footer..
 *
 * Available variables:
 * - $config: block settings.
 *
 */
if(isset($config)): ?>
<div id="opendata-footer">

	<?php if(isset($config['Disclaimer']['label']) && isset($config['Disclaimer']['link'])
			&& !empty($config['Disclaimer']['label']) && !empty($config['Disclaimer']['link'])): ?>
		<a id="disclaimer" href="<?php echo $config['Disclaimer']['link']; ?>" target="_blank"><?php echo $config['Disclaimer']['label']; ?></a>
	<?php endif; ?>
	
	<?php if(isset($config['Credits']['label']) && isset($config['Credits']['link'])
			&& !empty($config['Credits']['label']) && !empty($config['Credits']['link'])): ?>	
		<a id="credit" href="<?php echo $config['Credits']['link'];?>" target="_blank"><?php echo $config['Credits']['label'];?></a>
	<?php endif; ?>
	
	<?php if(isset($config['Copiright']['label']) && isset($config['Copiright']['link'])
			&& !empty($config['Copiright']['label']) && !empty($config['Copiright']['link'])): ?>
		<a id="copiright" href="<?php echo $config['Copiright']['link'];?>" target="_blank"><?php echo $config['Copiright']['label']; ?></a>
	<?php endif; ?>
	
	<?php if(isset($config['Privacy']['label']) && isset($config['Privacy']['link'])
			&& !empty($config['Privacy']['label']) && !empty($config['Privacy']['link'])): ?>
		<a id="privacy" href="<?php echo $config['Privacy']['link'];?>" target="_blank"><?php echo $config['Privacy']['label']; ?></a>
	<?php endif; ?>
	
	
</div>

<?php endif; ?>

	
