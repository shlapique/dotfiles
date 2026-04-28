let mapleader =" "

" pluginz BEGIN
set nocompatible                      " required
filetype off                          " required
set rtp+=~/.vim/bundle/Vundle.vim
call vundle#begin()

Plugin 'VundleVim/Vundle.vim'
Plugin 'junegunn/goyo.vim'
Plugin 'tpope/vim-commentary'
" Plugin 'JuliaEditorSupport/julia-vim'
" Plugin 'tomlion/vim-solidity'
Plugin 'tpope/vim-fireplace'
Plugin 'guns/vim-clojure-static' " syntax & indent
Plugin 'jpalardy/vim-slime'
" Plugin 'liquidz/elin'

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

" Tags
" search order:
" 1. local project 'tags'
" 2. parent dirs (../../../ ... )
" 3. language specific (go-tags, etc.)
set tags=tags;
" by lang BEGIN
augroup LanguageTags
  autocmd!
  autocmd FileType python setlocal tags+=./.tags/python.tags
  autocmd FileType go setlocal tags+=./.tags/go-mod.tags,~/.tags/go-stdlib.tags
  autocmd FileType scheme,lisp,clojure,racket,guile setlocal tags+=./.tags/scheme.tags,~/.tags/chicken.tags
augroup END
" by lang END
" BASE END

" filetypez BEGIN
autocmd FileType make setlocal noexpandtab shiftwidth=8 softtabstop=0 tabstop=8
autocmd FileType r setlocal ts=2 sts=2 sw=2 expandtab
autocmd FileType yaml setlocal ts=2 sts=2 sw=2 expandtab
autocmd FileType typescript setlocal ts=2 sts=2 sw=2 expandtab
" filetypez END

" goyo 
map <leader>f :Goyo \| set linebreak<CR>

" remap ctrl + c and ctrl + v
vnoremap <C-c> "*y :let @+=@*<CR>
map <C-p> "*p

" fireplace keymaps
autocmd FileType clojure nmap <buffer> <leader>ee cpp
" nmap <leader>ee cpp

" --- LISPs ---
" Scheme / Lisp basics
autocmd FileType scheme,lisp,clojure,racket,guile setlocal lisp
autocmd FileType scheme,lisp,clojure,racket,guile setlocal autoindent
autocmd FileType scheme,lisp,clojure,racket,guile setlocal showmatch
let g:slime_target = "tmux"
let g:slime_default_config = {"socket_name": "default", "target_pane": ":.1"}
let g:slime_bracketed_paste = 1
let g:slime_python_ipython = 0

function! SendForm()
  let l:pos = getpos(".")
  normal! vab
  normal! "sy
  call setpos('.', l:pos)
  call slime#send(@s . "\n")
endfunction

augroup LispSlime
  autocmd!
  autocmd FileType scheme,lisp,racket,guile nnoremap <buffer> <leader>ee :call SendForm()<CR>
augroup END
