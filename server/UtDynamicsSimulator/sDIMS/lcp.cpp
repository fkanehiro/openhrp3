/*!
 * @file   lcp.cpp
 * @author Katsu Yamane
 * @date   12/15/2006
 * @brief  Iterative solver implementation for Linear Complementarity Problems (LCP)
 */

#include "lcp.h"
#include <limits>
#include <list>

int LCP::Solve(fVec& _g, fVec& _a)
{
	fVec g_init(n_vars);
	g_init.zero();
	return SolveEx(_g, _a, g_init);
}

int LCP::SolveEx(fVec& _g, fVec& _a, const fVec& _g_init, double _max_error, int _max_iteration, double _speed, int* n_iteration)
{
	_g.resize(n_vars);
	_a.resize(n_vars);
	_g.set(_g_init);
	int failed = true, count = 0;;
	while(_max_iteration < 0 || count < _max_iteration)
	{
		for(int i=0; i<n_vars; i++)
		{
			double a = 0.0;
			for(int j=0; j<n_vars; j++)
			{
				a += N(i, j) * _g(j);
			}
			double z = _g(i) - _speed*(r(i)+a)/N(i, i);
			_g(i) = (z >= 0.0 ? z : 0.0);
		}
		_a.mul(N, _g);
		_a += r;
		if(_g.min_value() > -_max_error && _a.min_value() > -_max_error)
		{
			failed = false;
			break;
		}
		count++;
	}
	_a.mul(N, _g);
	_a += r;
	if(n_iteration) *n_iteration = count;
	return failed;
}

