<?php
/**
 * Template file: metadata_edit_authors.tpl.php
 * 
 * @param array $rows authors
 * @param array $form edit/add form 
 */

if(!isset($rows) && !isset($form))
	return;

$count = 0;
$logo_basepath = base_path() . drupal_get_path('module', 'metadata') . '/img/authors/';
?>
<ul class="action-links">
	<li>
		<a id="add-author" href="#">Aggiungi una Fonte</a>
	</li>
</ul>
<table id="authors-list">
<thead>
	<tr><td class="logo">Logo</td><td class="author">Fonte</td><td class="website">www</td><td class="email">Email</td><td class="action">Azione</td></tr>
</thead>
<tbody>
<?php foreach ($rows as $author) : 
	$count +=1;
	$row_class = $count%2==0 ? 'even' : 'odd';  
?>
	
	
	
	<tr id="item-<?php echo $author->id; ?>" class="<?php echo $row_class; ?>">
		<td class="logo"><img src="<?php echo $logo_basepath . $author->logo; ?>"/></td>
		<td class="author"><?php echo $author->name; ?></td>
		<td class="website"><?php echo $author->website; ?></td>
		<td class="email"><?php echo $author->mail; ?></td>
		<td class="action">
			<button data-id="<?php echo $author->id; ?>" class="edit action">Modifica</button>
			<br>
			<button data-id="<?php echo $author->id; ?>" class="delete action">Elimina</button>
		</td>
	</tr>
	
	
<?php endforeach; ?>

</tbody>
</table>
<div style="display:none">
<?php 

echo render($form);

echo render($form_delete);

?>
</div>