let mapleader =" "

" pluginz BEGIN
set nocompatible                      " be iMproved, required
filetype off                          " required
set rtp+=~/.vim/bundle/Vundle.vim
call vundle#begin()

Plugin 'VundleVim/Vundle.vim'
Plugin 'junegunn/goyo.vim'
Plugin 'tpope/vim-commentary'
Plugin 'JuliaEditorSupport/julia-vim'
Plugin 'tomlion/vim-solidity'
Plugin 'tpope/vim-fireplace'

call vundle#end()                      " required
filetype plugin indent on              " required
" pluginz END

" BASE BEGIN
syntax on
set expandtab
set tabstop=4
set shiftwidth=4
set smarttab
set autoindent
set smartindent
set number relativenumber
set incsearch
set hlsearch
set mouse=a

" generate tags with: ctags -R .
" check for tags in the directory of the current file
" and also check upward through parent directories until it finds a tags file
" or hits the root of the filesystem
set tags=./tags,tags;
" BASE END

" filetypez BEGIN
autocmd FileType make setlocal noexpandtab shiftwidth=8 softtabstop=0 tabstop=8
autocmd FileType r setlocal ts=2 sts=2 sw=2 expandtab
autocmd FileType yaml setlocal ts=2 sts=2 sw=2 expandtab
autocmd FileType typescript setlocal ts=2 sts=2 sw=2 expandtab
" filetypez END

" for autoclose braces
"inoremap { {}<Esc>ha
"inoremap ( ()<Esc>ha
"inoremap [ []<Esc>ha
"inoremap ' ''<Esc>ha
"inoremap ` ``<Esc>ha

" goyo 
map <leader>f :Goyo \| set linebreak<CR>

" remap ctrl + c and ctrl + v
vnoremap <C-c> "*y :let @+=@*<CR>
map <C-p> "*p
