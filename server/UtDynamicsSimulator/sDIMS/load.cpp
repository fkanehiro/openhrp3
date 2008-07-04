/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * load.cpp
 * Create: Katsu Yamane, 03.12.02
 */

#include "chain.h"

#define DEFAULT_LEX_LINE_BUFFER_SIZE	(64*1024)

void ClearBuffer();
void ChainMakeLexerBuffers(int lexBufferSize, int lineBufferSize);
void ChainSetInputFile(FILE *fp);
int chparse();
void ChainDeleteLexerBuffers();
void SetCharName(const char* chname);
void SetChain(Chain* ch);

int Chain::Load(const char* fname, const char* charname)
{
	ClearBuffer();
	FILE* fp = fopen(fname, "r");
	if(!fp)
	{
		cerr << "Chain::Load- error: failed to open " << fname << " to read" << endl;
		return -1;
	}

	fseek(fp, 0, SEEK_END);
	int lexBufferSize = ftell(fp);
	fseek(fp, 0, SEEK_SET);
	ChainMakeLexerBuffers(lexBufferSize, DEFAULT_LEX_LINE_BUFFER_SIZE);

	if(charname && strcmp(charname, ""))
		SetCharName(charname);
	SetChain(this);

	ChainSetInputFile(fp);
	int ret = chparse();

	ChainDeleteLexerBuffers();
	fclose(fp);

//	Dump(cout);
	return ret;
}
