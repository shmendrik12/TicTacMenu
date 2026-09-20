package com.example.tictacmenu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        String name = SessionStore.displayName(requireContext());
        ((TextView) view.findViewById(R.id.profile_name)).setText(
                name.isEmpty() ? getString(R.string.guest) : name);
        view.findViewById(R.id.profile_sign_out).setOnClickListener(button ->
                ((MenuActivity) requireActivity()).logoutAndOpenLogin());
        return view;
    }
}
