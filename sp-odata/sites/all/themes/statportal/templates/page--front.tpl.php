<?php

/**
 * @file
 * statportal's theme implementation to display a single Drupal page.
 *
 * The doctype, html, head and body tags are not in this template. Instead they
 * can be found in the html.tpl.php template normally located in the
 * modules/system folder.
 *
 * Available variables:
 *
 * General utility variables:
 * - $base_path: The base URL path of the Drupal installation. At the very
 *   least, this will always default to /.
 * - $directory: The directory the template is located in, e.g. modules/system
 *   or themes/statportal.
 * - $is_front: TRUE if the current page is the front page.
 * - $logged_in: TRUE if the user is registered and signed in.
 * - $is_admin: TRUE if the user has permission to access administration pages.
 *
 * Site identity:
 * - $front_page: The URL of the front page. Use this instead of $base_path,
 *   when linking to the front page. This includes the language domain or
 *   prefix.
 * - $logo: The path to the logo image, as defined in theme configuration.
 * - $site_name: The name of the site, empty when display has been disabled
 *   in theme settings.
 * - $site_slogan: The slogan of the site, empty when display has been disabled
 *   in theme settings.
 * - $hide_site_name: TRUE if the site name has been toggled off on the theme
 *   settings page. If hidden, the "element-invisible" class is added to make
 *   the site name visually hidden, but still accessible.
 * - $hide_site_slogan: TRUE if the site slogan has been toggled off on the
 *   theme settings page. If hidden, the "element-invisible" class is added to
 *   make the site slogan visually hidden, but still accessible.
 *
 * Navigation:
 * - $main_menu (array): An array containing the Main menu links for the
 *   site, if they have been configured.
 * - $secondary_menu (array): An array containing the Secondary menu links for
 *   the site, if they have been configured.
 * - $breadcrumb: The breadcrumb trail for the current page.
 *
 * Page content (in order of occurrence in the default page.tpl.php):
 * - $title_prefix (array): An array containing additional output populated by
 *   modules, intended to be displayed in front of the main title tag that
 *   appears in the template.
 * - $title: The page title, for use in the actual HTML content.
 * - $title_suffix (array): An array containing additional output populated by
 *   modules, intended to be displayed after the main title tag that appears in
 *   the template.
 * - $messages: HTML for status and error messages. Should be displayed
 *   prominently.
 * - $tabs (array): Tabs linking to any sub-pages beneath the current page
 *   (e.g., the view and edit tabs when displaying a node).
 * - $action_links (array): Actions local to the page, such as 'Add menu' on the
 *   menu administration interface.
 * - $feed_icons: A string of all feed icons for the current page.
 * - $node: The node object, if there is an automatically-loaded node
 *   associated with the page, and the node ID is the second argument
 *   in the page's path (e.g. node/12345 and node/12345/revisions, but not
 *   comment/reply/12345).
 *
 * Regions:
 * - $page['header']: Items for the header region.
 * - $page['featured']: Items for the featured region.
 * - $page['highlighted']: Items for the highlighted content region.
 * - $page['help']: Dynamic help text, mostly for admin pages.
 * - $page['content']: The main content of the current page.
 * - $page['sidebar_first']: Items for the first sidebar.
 * - $page['triptych_first']: Items for the first triptych.
 * - $page['triptych_middle']: Items for the middle triptych.
 * - $page['triptych_last']: Items for the last triptych.
 * - $page['footer_firstcolumn']: Items for the first footer column.
 * - $page['footer_secondcolumn']: Items for the second footer column.
 * - $page['footer_thirdcolumn']: Items for the third footer column.
 * - $page['footer_fourthcolumn']: Items for the fourth footer column.
 * - $page['footer']: Items for the footer region.
 *
 * @see template_preprocess()
 * @see template_preprocess_page()
 * @see template_process()
 * @see statportal_process_page()
 */

$theme_path = drupal_get_path('theme', 'statportal');
// drupal_add_js($theme_path . '/js/jquery.jshowoff.mod.js');
// drupal_add_js($theme_path . '/js/home.js');
drupal_add_js($theme_path . '/js/jquery.barousel.min.js');
drupal_add_js($theme_path . '/js/mybarousel.js');
drupal_add_css($theme_path . '/css/home.css', array('type' => 'file', 'group' => CSS_THEME));
drupal_add_css($theme_path . '/css/barousel.css', array('type' => 'file', 'group' => CSS_THEME));
drupal_add_js($theme_path  . '/js/page.js', array('weight' => -20, 'group' => JS_THEME));

// bug fix
if(!isset($hide_site_name))
	$hide_site_name = false;
$hide_site_slogan=$hide_site_name;

//echo 'Personalizza home page da page--font.tpl.php';
?>
<div id="page-wrapper"><div id="page">

  <div id="header" class="<?php print $secondary_menu ? 'with-secondary-menu': 'without-secondary-menu'; ?>"><div class="section clearfix">
	<?php
	 render($page['content']) 
	?>
    <?php if ($logo): ?>
      <a href="<?php print $front_page; ?>" title="<?php print t('Home'); ?>" rel="home" id="logo">
        <img src="<?php print $logo; ?>" alt="<?php print t('Home'); ?>" />
      </a>
    <?php endif; ?>
	
	<?php print render($page['header']); ?>
	
    <?php if ($site_name || $site_slogan): ?>
      <div id="name-and-slogan"<?php if ($hide_site_name && $hide_site_slogan) { print ' class="element-invisible"'; } ?>>

        <?php if ($site_name): ?>
          <?php if ($title): ?>
            <div id="site-name"<?php if ($hide_site_name) { print ' class="element-invisible"'; } ?>>
              <strong>
                <a href="<?php print $front_page; ?>" title="<?php print t('Home'); ?>" rel="home"><span><?php print $site_name; ?></span></a>
              </strong>
            </div>
          <?php else: /* Use h1 when the content title is empty */ ?>
            <h1 id="site-name"<?php if ($hide_site_name) { print ' class="element-invisible"'; } ?>>
              	<a href="<?php print $front_page; ?>" title="<?php print t('Home'); ?>" rel="home"><span><?php print $site_name; ?></span></a>
				<?php if ($site_slogan): ?>
		      	<div id="site-slogan"<?php if ($hide_site_slogan) { print ' class="element-invisible"'; } ?>>
		            <?php print $site_slogan; ?>
				</div>
		      	<?php endif; ?>
            </h1>
          <?php endif; ?>
        <?php endif; ?>

        

      </div> <!-- /#name-and-slogan -->
    <?php endif; ?>

    <div id="language" class="language"> 
<?php print render($page['language']); ?> 
</div> 

    <?php if ($secondary_menu): ?>
      <div id="secondary-menu" class="navigation">
        <?php print theme('links__system_secondary_menu', array(
          'links' => $secondary_menu,
          'attributes' => array(
            'id' => 'secondary-menu-links',
            'class' => array('links', 'inline', 'clearfix'),
          ),
          'heading' => array(
            'text' => t('Secondary menu'),
            'level' => 'h2',
            'class' => array('element-invisible'),
          ),
        )); ?>
      </div> <!-- /#secondary-menu -->
    <?php endif; ?>
  </div></div> <!-- /.section, /#header -->
 

    <?php if ($main_menu): ?>
      <div id="main-menu" class="navigation">
        <?php print theme('links__system_main_menu', array(
          'links' => $main_menu,
          'attributes' => array(
            'id' => 'main-menu-links',
            'class' => array('links', 'clearfix'),
          ),
          'heading' => array(
            'text' => t('Main menu'),
            'level' => 'h2',
            'class' => array('element-invisible'),
          ),
        )); ?>
      </div> <!-- /#main-menu -->
    <?php endif; ?>
    
    
  <?php if ($messages): ?>
    <div id="messages"><div class="section clearfix">
      <?php print $messages; ?>
    </div></div> <!-- /.section, /#messages -->
  <?php endif; ?>

  <?php if ($page['featured']): ?>
    <div id="featured"><div class="section clearfix">
      <?php print render($page['featured']); ?>
    </div></div> <!-- /.section, /#featured -->
  <?php endif; ?>

  <div id="main-wrapper" class="clearfix"><div id="main" class="clearfix">

    <div id="front-content" class="column">
    	    	
    	<div id="left-content">

	<?php if (isset($page['home_news_barousel']) && count($page['home_news_barousel'])) : ?>
		<div id="any_id" class="barousel">
		    <?php print render($page['home_news_barousel']); ?>
		</div>	
	<?php endif; ?>

    	
    	
			<div id="shortcut-content">
				<?php if (isset($page['home_box_news']) && count($page['home_box_news'])) : ?>	
					<div class='info_box'  id="odata-tweets">
						<!-- Tweeter code -->
						<script charset="utf-8" src="http://widgets.twimg.com/j/2/widget.js"></script>
						<?php print render($page['home_box_news']); ?>
						<!-- End Tweeter code -->
					</div>
				<?php endif; ?>
				<div class='info_box'  id="odata-search">
					<h3>Esplora dati</h3>
					Cerca i dati nel nostro catalogo ed esplora le diverse categorie:
					<ul>
						<li><a href='<?php print url("catalog///" . ORDER_BY_MOST_SEARCHED);?>'>Dati pi&ugrave; ricercati</a></li>
						<li><a href='<?php print url("catalog///" . ORDER_BY_MOST_DOWNLOADED);?>'>Dati pi&ugrave; scaricati</a></li>
						<li><a href='<?php print url("catalog///" . ORDER_BY_RATING);?>'>Dati pi&ugrave; votati</a></li>
						<li><a href='<?php print url("catalog///" . ORDER_BY_LAST_UPDATE);?>'>Dati pi&ugrave; recenti</a></li>
					</ul>	
					Oppure fai una <a href='<?php print url("catalog");?>'>ricerca mirata</a>.

				</div>
     			
    			
    			<div class='info_box' id="odata-upload">
    				<h3>Carica un dato</h3>
					<b>Prossimamente!</b><br />
					Tieniti sintonizzato: a breve attiveremo la registrazione account e potrai caricare i tuoi dati. <br /><br />

					Per saperne di pi&ugrave; vai alla pagina <a href='http://www.datiopen.it/it/caricamentoDati'> caricamento dati </a><br />
					
					oppure	<a href='mailto:dati@datiopen.it'> Contattaci</a>					
    			</div>
    			
	    		<?php if (isset($page['home_box_content_1']) && count($page['home_box_content_1'])) : ?>
	    			<div class="info_box" id="odata-box-1">
	    				<?php print render($page['home_box_content_1']); ?>
	    			</div>
	    		<?php endif; ?>
    			
    			<?php if (isset($page['home_box_content_2']) && count($page['home_box_content_2'])) : ?>
	    			<div class="info_box" id="odata-box-2">
	    				<?php print render($page['home_box_content_2']); ?>
	    			</div>
    			<?php endif; ?>
    			
    			
    			
			</div>  
			<?php if (isset($page['home_article']) && count($page['home_article'])) : ?>
				<div class="article_box" id="odata-article-box">
	    			<?php print render($page['home_article']); ?>
	    		</div>
	    	<?php endif; ?>
    	</div>
    	
    	<div id="right-content">

    		
    			<?php if ($page['home_top_right']): ?>
			      <div id="top-shortcut" class="column sidebar">
			        <?php print render($page['home_top_right']); ?>
			      </div> <!-- /.section, /#home_bottom_right -->
			    <?php endif; ?>
			
    		<!--  
    			<div id="top-shortcut">
	    			<div class="category-link" id="agricoltura"></div>
	    			<div class="category-link" id="ambiente"></div>
	    			<div class="category-link" id="areetematiche"></div>
	    			<div class="category-link" id="cultura"></div>
	    			
	    			<div class="category-link" id="rifiuti"></div>
	    			<div class="category-link" id="sicurezza"></div>
	    			<div class="category-link" id="infanzia"></div>
	    			<div class="category-link" id="impresa"></div>
    			</div>
    		-->
    		<div id="bottom-shortcut">
				<?php if ($page['home_bottom_right']): ?>
			      <div id="home_bottom_right" class="column sidebar"><div class="section">
			        <?php print render($page['home_bottom_right']); ?>
			      </div></div> <!-- /.section, /#home_bottom_right -->
			    <?php endif; ?>
    		</div>
    	</div>  
   
    
    </div> <!-- /.section /#content -->
    <?php if ($page['sidebar_second']): ?>
      <div id="sidebar-second" class="column sidebar"><div class="section">
        <?php print render($page['sidebar_second']); ?>
      </div>
      </div> <!-- /.section, /#sidebar-second -->
    <?php endif; ?>

  </div></div> <!-- /#main, /#main-wrapper -->

  <?php if ($page['triptych_first'] || $page['triptych_middle'] || $page['triptych_last']): ?>
    <div id="triptych-wrapper"><div id="triptych" class="clearfix">
      <?php print render($page['triptych_first']); ?>
      <?php print render($page['triptych_middle']); ?>
      <?php print render($page['triptych_last']); ?>
    </div></div> <!-- /#triptych, /#triptych-wrapper -->
  <?php endif; ?>

  <div id="footer-wrapper"><div class="section">

    <?php if ($page['footer_firstcolumn'] || $page['footer_secondcolumn'] || $page['footer_thirdcolumn'] || $page['footer_fourthcolumn']): ?>
      <div id="footer-columns" class="clearfix">
        <?php print render($page['footer_firstcolumn']); ?>
        <?php print render($page['footer_secondcolumn']); ?>
        <?php print render($page['footer_thirdcolumn']); ?>
        <?php print render($page['footer_fourthcolumn']); ?>
      </div> <!-- /#footer-columns -->
    <?php endif; ?>

    <?php if ($page['footer']): ?>
      <div id="footer" class="clearfix">
        <?php print render($page['footer']); ?>
      </div> <!-- /#footer -->
    <?php endif; ?>

  </div></div> <!-- /.section, /#footer-wrapper -->

</div></div> <!-- /#page, /#page-wrapper -->
<div id="fb-root"></div>
<script>(function(d, s, id) {
  var js, fjs = d.getElementsByTagName(s)[0];
  if (d.getElementById(id)) return;
  js = d.createElement(s); js.id = id;
  js.src = "//connect.facebook.net/it_IT/all.js#xfbml=1";
  fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));</script>