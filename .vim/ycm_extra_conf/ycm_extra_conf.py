def FlagsForFile( filename, **kwargs ):
    return {
            'flags': [ '-c', '-Wall', '-Wextra', '-Werror', 'std=c++1z' ]
    }
