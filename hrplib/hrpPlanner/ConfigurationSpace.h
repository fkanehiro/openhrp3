// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#ifndef __CONFIGURATION_SPACE_H__
#define __CONFIGURATION_SPACE_H__

#include "exportdef.h"
#include <vector>
#include <iostream>

namespace PathEngine {
    class Configuration;

    class HRPPLANNER_API ConfigurationSpace
    {
    public:
        /**
           @brief constructor
        */
        ConfigurationSpace(unsigned int i_size);

        /**
         * @brief 全ての要素が有効な範囲内にあるかどうか検査する
         * @return 全ての要素が有効範囲内にあればtrue、それ以外false
         */
        bool isValid(const Configuration& cfg) const;

        /**
         * @brief get weight for i_rank th element
         * @param i_rank rank of the element
         * @return weight
         */
        double &weight(unsigned int i_rank);
        const double weight(unsigned int i_rank) const;

        /**
         * @brief set bounds for i_rank th element
         * @param i_rank rank of the element
         * @param min minimum value
         * @param max maximum value
         */
        void bounds(unsigned int i_rank, double min, double max);

        /**
         * @brief get upper bound for i_rank th element
         * @param i_rank rank of the element
         * @return upper bound
         */
        double& ubound(unsigned int i_rank);

        /**
         * @brief get lower bound of \e i_rank th element
         * @param i_rank rank of the element
         * @return lower bound
         */
        double& lbound(unsigned int i_rank);

        /**
           @brief specify \e i th degree of freedom is unbounded rotaion or not. default is false
           @param i_rank rank of the degree of freedom
           @param i_flag true or false
         */
        void unboundedRotation(unsigned int i_rank, bool i_flag);

        /**
           @brief ask \e i the degree of freedom is unbounded rotaion or not
           @param i_rank rank of the degree of freedom
           @return true(unbounded) or false
         */
        bool unboundedRotation(unsigned int i_rank);

        /**
         * @brief generate random position
         * @return generated position
         */
        Configuration random();

        /**
           @brief get the number of degrees of freedom
           @return the number of degrees of freedom
         */
        unsigned int size();

    private:    
        unsigned int m_size;
        std::vector<double> m_ubounds;
        std::vector<double> m_lbounds;
        std::vector<double> m_weights;
        std::vector<bool> m_isUnboundedRotation;
    };
};


#endif
