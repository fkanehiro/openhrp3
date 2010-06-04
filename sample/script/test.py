a = 1
b = 10
waitInputSetMessage("$1= test ")
waitInputMenu([["A","print a","B","print b",
                '----------label----------', '#label',
                'string input', 'print #T',
                'double input', 'print #D,#D',
                'int input', 'print #I,#I,#I',
                ],
               ["A*10","print a*10","B*10","print b*10"],
               ["A*100","print a*100","B*100","print b*100"]])


