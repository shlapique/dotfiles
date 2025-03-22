let mapleader =" "

" pluginz
set nocompatible              " be iMproved, required
filetype off                  " required
set rtp+=~/.vim/bundle/Vundle.vim
call vundle#begin()

Plugin 'VundleVim/Vundle.vim'
Plugin 'junegunn/goyo.vim'
Plugin 'tpope/vim-commentary'
Plugin 'JuliaEditorSupport/julia-vim'
Plugin 'tomlion/vim-solidity'

call vundle#end()            " required
filetype plugin indent on    " required
" pluginz END

" BASE
set tabstop=4
set shiftwidth=4
set smarttab
set expandtab
set smartindent
set number relativenumber
set incsearch
set hlsearch
set mouse=a
syntax enable

autocmd FileType make setlocal shiftwidth=8 noexpandtab
autocmd FileType r setlocal ts=2 sts=2 sw=2 expandtab
autocmd FileType yaml setlocal ts=2 sts=2 sw=2 expandtab

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

" Function to toggle TODO-DONE in .todo files
function! ToggleTodo()
    let l:line = getline('.')

    if l:line =~ '^TODO '
        let l:new_line = substitute(l:line, '^TODO ', '✅ ', '')
    elseif l:line =~ '^✅ '
        let l:new_line = substitute(l:line, '^✅ ', '', '')
    else
        let l:new_line = 'TODO ' . l:line
    endif

    call setline('.', l:new_line)
endfunction
nnoremap gt :call ToggleTodo()<CR>
autocmd BufNewFile,BufRead *.todo set filetype=todo
