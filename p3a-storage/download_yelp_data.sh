declare -a data_files=(businesses checkin review tip user)
for data_file in "${data_files[@]}"; do
  wget https://clouddeveloper.blob.core.windows.net/datasets/cloud-storage/yelp_academic_dataset_"$data_file".tsv
done
