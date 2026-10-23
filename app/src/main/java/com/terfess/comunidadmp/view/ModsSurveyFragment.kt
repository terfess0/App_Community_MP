package com.terfess.comunidadmp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentModsSurveyBinding
import com.terfess.comunidadmp.viewmodel.ModsSurveyViewModel

class ModsSurveyFragment : Fragment() {

    private var _binding: FragmentModsSurveyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ModsSurveyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModsSurveyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = Firebase.auth.currentUser
        val userEmail = if (currentUser != null && !currentUser.isAnonymous) currentUser.email ?: "" else ""

        // Verificar si el usuario ya votó
        viewModel.checkUserVote(userEmail)

        setObservers()

        binding.btnSubmitVote.setOnClickListener {
            handleVoteSubmit()
        }
    }

    private fun setObservers() {
        viewModel.hasUserVoted.observe(viewLifecycleOwner) { hasVoted ->
            if (hasVoted) {
                binding.cardVotingForm.visibility = View.GONE
                binding.cardResults.visibility = View.VISIBLE
                binding.tvStatusVoted.text = "¡Ya has participado en este sondeo!"
            } else {
                binding.cardVotingForm.visibility = View.VISIBLE
            }
        }

        viewModel.voteSubmitted.observe(viewLifecycleOwner) { success ->
            binding.progressVote.visibility = View.GONE
            binding.btnSubmitVote.visibility = View.VISIBLE
            if (success) {
                binding.cardVotingForm.visibility = View.GONE
                binding.cardResults.visibility = View.VISIBLE
                binding.tvStatusVoted.text = "¡Tu voto ha sido registrado con éxito!"
                Snackbar.make(requireView(), "¡Gracias por participar en el sondeo!", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.surveyStats.observe(viewLifecycleOwner) { stats ->
            val yes = stats.first
            val total = stats.second
            val percent = if (total > 0) (yes * 100) / total else 0
            binding.tvStatsYes.text = "A favor de añadir mods: $yes ($percent%)"
            binding.tvStatsTotal.text = "Total de votos comunitarios: $total"
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            binding.progressVote.visibility = View.GONE
            binding.btnSubmitVote.visibility = View.VISIBLE
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVoteSubmit() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null || currentUser.isAnonymous || currentUser.email.isNullOrBlank()) {
            Utilities().showAlert(
                requireContext(),
                "Inicia Sesión",
                "Para participar en la encuesta de la comunidad debes iniciar sesión.",
                "Iniciar sesión",
                "Cancelar",
                false,
                {
                    findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
                },
                {}
            )
            return
        }

        if (!binding.checkUnderstandRisks.isChecked) {
            Toast.makeText(
                requireContext(),
                "Debes marcar la casilla aceptando que reconoces los riesgos.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val wantsMods = binding.radioYes.isChecked
        val preferredType = when (binding.radioGroupModType.checkedRadioButtonId) {
            R.id.radioSkins -> "Skins"
            R.id.radioVehicles -> "Vehículos & Texturas"
            R.id.radioHud -> "HUDs & Radares"
            R.id.radioAudio -> "Sonidos & Efectos"
            else -> "General"
        }

        binding.btnSubmitVote.visibility = View.GONE
        binding.progressVote.visibility = View.VISIBLE

        // Verificar si está baneado
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("userEmail", currentUser.email)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty && query.documents[0].getBoolean("isBanned") == true) {
                    binding.progressVote.visibility = View.GONE
                    binding.btnSubmitVote.visibility = View.VISIBLE
                    val reason = query.documents[0].getString("banReason") ?: ""
                    Utilities().showAlert(
                        requireContext(),
                        "Cuenta Suspendida",
                        "Tu cuenta está suspendida y no puede votar.\nMotivo: $reason",
                        "Entendido",
                        null,
                        false,
                        {},
                        {}
                    )
                    return@addOnSuccessListener
                }

                val voteData = hashMapOf(
                    "userEmail" to currentUser.email,
                    "wantsMods" to wantsMods,
                    "preferredType" to preferredType,
                    "date" to Timestamp.now()
                )

                viewModel.submitVote(voteData)
            }
            .addOnFailureListener {
                binding.progressVote.visibility = View.GONE
                binding.btnSubmitVote.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Error al validar cuenta", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
