// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-

#ifndef TVMET3D_H_INCLUDED
#define TVMET3D_H_INCLUDED

#include "tvmetCommonTypes.h"
#include "hrpModelExportDef.h"

namespace OpenHRP
{
	namespace PRIVATE
	{
		HRPMODEL_EXPORT void rodrigues(matrix33& out_R, const vector3& axis, double q);
		HRPMODEL_EXPORT void rotFromRpy(matrix33& out_R, double r, double p, double y);
	};

    inline matrix33 rodrigues(const vector3& axis, double q){
		matrix33 R;
		PRIVATE::rodrigues(R, axis, q);
		return R;
	}

	inline matrix33 rotFromRpy(const vector3& rpy){
		matrix33 R;
		PRIVATE::rotFromRpy(R, rpy[0], rpy[1], rpy[2]);
		return R;
	}

	inline matrix33 rotFromRpy(double r, double p, double y){
		matrix33 R;
		PRIVATE::rotFromRpy(R, r, p, y);
		return R;
	}
	
    HRPMODEL_EXPORT vector3 omegaFromRot(const matrix33& r);
	HRPMODEL_EXPORT vector3 rpyFromRot(const matrix33& m);
	HRPMODEL_EXPORT matrix33 inverse33(const matrix33& m);

    inline matrix33 hat(const vector3& c) {
		matrix33 m;
		m = 0.0,  -c(2),  c(1),
			c(2),  0.0,  -c(0),
			-c(1),  c(0),  0.0;
		return m;
    }

	inline matrix33 VVt_prod(const vector3& a, const vector3& b){
		matrix33 m;
		m = a(0) * b(0), a(0) * b(1), a(0) * b(2),
			a(1) * b(0), a(1) * b(1), a(1) * b(2),
			a(2) * b(0), a(2) * b(1), a(2) * b(2);
		return m;
	}

    template<class M> inline void setMatrix33(const matrix33& m33, M& m, size_t row = 0, size_t col = 0){
		m(row, col) = m33(0, 0); m(row, col+1) = m33(0, 1); m(row, col+2) = m33(0, 2);
		++row;
		m(row, col) = m33(1, 0); m(row, col+1) = m33(1, 1); m(row, col+2) = m33(1, 2);
		++row;
		m(row, col) = m33(2, 0); m(row, col+1) = m33(2, 1); m(row, col+2) = m33(2, 2);
    }
	
    template<class M> inline void setTransMatrix33(const matrix33& m33, M& m, size_t row = 0, size_t col = 0){
		m(row, col) = m33(0, 0); m(row, col+1) = m33(1, 0); m(row, col+2) = m33(2, 0);
		++row;
		m(row, col) = m33(0, 1); m(row, col+1) = m33(1, 1); m(row, col+2) = m33(2, 1);
		++row;
		m(row, col) = m33(0, 2); m(row, col+1) = m33(1, 2); m(row, col+2) = m33(2, 2);
    }
	
    template<class Array> inline void setMatrix33ToRowMajorArray
	(const matrix33& m33, Array& a, size_t top = 0) {
		a[top++] = m33(0, 0);
		a[top++] = m33(0, 1);
		a[top++] = m33(0, 2);
		a[top++] = m33(1, 0);
		a[top++] = m33(1, 1);
		a[top++] = m33(1, 2);
		a[top++] = m33(2, 0);
		a[top++] = m33(2, 1);
		a[top  ] = m33(2, 2);
    }

    template<class Array> inline void getMatrix33FromRowMajorArray
	(matrix33& m33, const Array& a, size_t top = 0) {
		m33(0, 0) = a[top++];
		m33(0, 1) = a[top++];
		m33(0, 2) = a[top++];
		m33(1, 0) = a[top++];
		m33(1, 1) = a[top++];
		m33(1, 2) = a[top++];
		m33(2, 0) = a[top++];
		m33(2, 1) = a[top++];
		m33(2, 2) = a[top  ];
    }

    template<class V> inline void setVector3(const vector3& v3, V& v, size_t top = 0){
		v[top++] = v3(0); v[top++] = v3(1); v[top] = v3(2);
    }

	template<class V> inline void setVector3(const vector3& v3, const V& v, size_t top = 0){
		v[top++] = v3(0); v[top++] = v3(1); v[top] = v3(2);
    }

    template<class V> inline void getVector3(vector3& v3, const V& v, size_t top = 0){
		v3(0) = v[top++]; v3(1) = v[top++]; v3(2) = v[top]; 
    }
	
    template<class M> inline void setVector3(const vector3& v3, M& m, size_t row, size_t col){
		m(row++, col) = v3(0); m(row++, col) = v3(1); m(row, col) = v3(2); 
    }

    template<class M> inline void getVector3(vector3& v3, const M& m, size_t row, size_t col){
		v3(0) = m(row++, col);
		v3(1) = m(row++, col);
		v3(2) = m(row, col);
    }

};

#endif
