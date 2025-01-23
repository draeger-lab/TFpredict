# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

project = 'TFpredict'
copyright = '2025, Johannes Eichner, Florian Topf, Andreas Dräger, James T. Yurkovich, Michael Römer, Dóra V. Molnár, Michael Gaas'
author = 'Johannes Eichner, Florian Topf, Andreas Dräger, James T. Yurkovich, Michael Römer, Dóra V. Molnár, Michael Gaas'
release = '1.4'

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

extensions = ['sphinx_copybutton']

templates_path = ['_templates']
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store', 'sidebar.rst']



# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'sphinx_rtd_theme'
html_theme_options = {
    "collapse_navigation": False,  # Set to False to expand the current TOC level.
    "navigation_depth": 4,         # Adjust depth to your content's structure.
}
html_sidebars = {
    '**': ['globaltoc.html', 'relations.html', 'sidebar.html', 'searchbox.html'],
}

html_logo = "_static/tfpredict_logo.png"

html_css_files = [
    'custom.css',
]

html_static_path = ['_static']
