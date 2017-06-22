'''
Created on Jun 21, 2017

@author: sarker
'''


import sys
import os

import numpy as np

root_path = '/Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images'


all_targets = []
all_parts = []
all_targets_set = set()
all_parts_set = set()


def print_infos():
    all_targets_set = set(all_targets)
    all_parts_set = set(all_parts)
    all_targets_cumulative = set(all_targets_set)
    all_targets_cumulative |= all_parts_set
    all_targets_cumulative = sorted(all_targets_cumulative)
    for target in all_targets_cumulative:
        print('target: ', target)
#     print('#########')
#     for part in all_parts:
#         print('parts: ', part)


def write_infos():
    all_targets_set = set(all_targets)
    all_parts_set = set(all_parts)
    all_terms_cumulative = set(all_targets_set)
    all_terms_cumulative |= all_parts_set
    all_terms_cumulative = sorted(all_terms_cumulative)
    with open('terms.txt', 'w', encoding='utf-8') as f:
        for element in all_terms_cumulative:
            f.write(element + '\n')
    print('len(all_terms_cumulative): ', len(all_terms_cumulative))


def iterate_over_a_folder(path):
    print('path: ', path)
    if not path.startswith('.'):
        # is it is directory
        if os.path.isdir(path):
            # save folder names
            if path.endswith('indoor') or path.endswith('outdoor'):
                name_parts = path.split('/')
                this_folder_name = name_parts[-1] + \
                    '_' + name_parts[-2]
            else:
                this_folder_name = path.split('/')[-1]
            all_targets.append(this_folder_name)

            for file_name in os.listdir(path):
                if not file_name.startswith('.'):
                    full_path = os.path.join(path, file_name)

                    # check whether it is file or directory
                    if os.path.isdir(full_path):
                        iterate_over_a_folder(full_path)
                    elif full_path.endswith('.txt'):
                        lines = open(os.path.join(path, file_name),
                                     mode='r', encoding='utf-8').readlines()
                        for line in lines:
                            parts = [x.strip()
                                     for x in line.split('#')[3].split(',')]
                            all_parts.extend(parts)


def start_iterating():
    for _folder_name in os.listdir(root_path):
        if not _folder_name.startswith('.'):
            # will get folder name training, validation etc
            # for exmaple:
            # /Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images/training
            # /Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images/validation
            # ...
            # etc
            _dir = os.path.join(root_path, _folder_name)
            print('_dir: ', _dir)
            for folder_name in os.listdir(_dir):
                if not folder_name.startswith('.'):
                    dir = os.path.join(_dir, folder_name)
                    # will get folder name a, b etc
                    # for exmaple:
                    # /Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images/training/a
                    # /Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images/training/b
                    # ...
                    # etc
                    print('dir: ', dir)
                    for inner_folder_name in os.listdir(dir):
                        if not inner_folder_name.startswith('.'):
                            inner_dir = os.path.join(dir, inner_folder_name)
                            # call with abbey, access_road etc
                            # for exmaple:
                            # /Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images/training/a/abbey
                            # /Users/sarker/Desktop/ProjectHCBD/Datas/ADE20K_2016_07_26/images/training/b/access_road
                            # ...
                            # etc
                            iterate_over_a_folder(inner_dir)


def cross_check_with_xie():
    all_targets_xie = []
    all_parts_xie = []
    with open('targets.txt', mode='r', encoding='utf-8') as f:
        terms = [x.replace('\n', '').replace('\'', '').strip().lower()
                 for x in f.readlines()]
        all_targets_xie.extend(terms)
    #print('all_targets_xie: ', all_targets_xie)

    with open('parts.txt', mode='r', encoding='utf-8') as f:
        parts = [item.strip().lower() for x in f.readlines()
                 for item in x.replace('\n', '').replace('\'', '').split(',')]
        all_parts_xie.extend(parts)
    #print('all_parts_xie: ', all_parts_xie)

    all_terms_xie = []
    all_terms_xie.extend(all_targets_xie)
    all_terms_xie.extend(all_parts_xie)
    # remove duplicates
    all_terms_xie = set(all_terms_xie)
    # sort
    all_terms_xie = sorted(all_terms_xie)
    with(open('terms_xie.txt', mode='w', encoding='utf-8')) as f:
        for element in all_terms_xie:
            f.write(element + '\n')
    print('len(all_terms_xie) :', len(all_terms_xie))


start_iterating()
# print_infos()
write_infos()
cross_check_with_xie()
