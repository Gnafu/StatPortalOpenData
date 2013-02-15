<?php 


$element = $variables['element'];

// Special handling for form elements.
if (isset($element['#array_parents'])) {
	// Assign an html ID.
	if (!isset($element['#attributes']['id'])) {
		$element['#attributes']['id'] = $element['#id'];
	}
	// Add the 'form-wrapper' class.
	$element['#attributes']['class'][] = 'form-wrapper';
}

print '<div' . drupal_attributes($element['#attributes']) . '>' . $element['#children'] . '</div>';


?>
