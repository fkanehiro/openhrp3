/*!
 * @file   common.h
 * @author Katsu Yamane
 * @date   06/18/2003
 * @brief  Defines convenient macros used throughout the project.
 */

#ifndef __COMMON_H__
#define __COMMON_H__

static char charname_separator = ':';

#ifndef PI
#define PI 3.1416
#endif

#define TINY 1e-8

#define MAX(m, n) ((m >= n) ? m : n)
#define MIN(m, n) ((m <= n) ? m : n)
#define MAX3(l, m, n) ((MAX(l,m) >= n) ? MAX(l,m) : n)

#endif
